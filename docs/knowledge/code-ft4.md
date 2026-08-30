# FT4 library source study (GitLab chromaway/ft4-lib)

**As-of:** 2026-08-26 (Asia/Jerusalem).
**Method:** GitLab API + raw file fetches only. No git clone.
**Primary tag:** v1.1.0r (commit 1dd829052f422c7a78c18aaebcf72967b7002212). Official docs pin this tag.
**Rule:** Names below are copied from that tag unless a later tag is named. Nothing invented.

Repo: https://gitlab.com/chromaway/ft4-lib  Project id 59553401. Default branch development.
Rell path: rell/src/lib/ft4. JS client path: client/lib/ft4. Package name: @chromia/ft4.

---
## 0. Tags vs official docs

GitLab tags observed 2026-08-26 via API.

version.rell contents:
@mount('ft4')
module;

/** Returns the version of the Rell FT4 library */
query get_version(): text = "1.1.0";

/** Returns the API version of the Rell FT4 library */
query get_api_version(): integer = 1;

---
## 1. Public import wrappers

### rell/src/lib/ft4/accounts/module.rell
import ^.core.accounts.*;
import external: ^.external.accounts;

### rell/src/lib/ft4/auth/module.rell
import ^.core.auth.*;
import external: ^.external.auth;

### rell/src/lib/ft4/assets/module.rell
import ^.core.assets.*;
import external: ^.external.assets;

### rell/src/lib/ft4/crosschain/module.rell
import ^.core.crosschain.*;
import external: ^.external.crosschain;

### rell/src/lib/ft4/admin/module.rell
import ^.core.admin.*;
import external: ^.external.admin;

### rell/src/lib/ft4/admin/crosschain/module.rell
import external: ^^.external.admin.crosschain;

### rell/src/lib/ft4/module.rell
/**
 * Defines general configurations that will be used by all modules
 */
struct module_args {
    /**
     * How big can pages be in paginated queries at max
     * 
     * @see utils.paged_result for more info on pagination
     */
    query_max_page_size: integer = 100;
}

/**
 * Utility function to retrieve the general configs in inner modules
 */
function get_module_args() {
    return chain_context.args;
}

---
## 2. Core accounts declarations

### core/accounts/module.rell
File: rell/src/lib/ft4/core/accounts/module.rell

9: val GTV_NULL = null.to_gtv();
11: val GTV_NULL_BYTES = null.to_gtv().to_bytes();
17: struct rate_limit_config {
32: struct auth_descriptor_config {
45: struct module_args {
55: val AUTH_DESCRIPTORS_PER_ACCOUNT_UPPER_BOUND = min(
61: function get_auth_descriptor_config() = chain_context.args.auth_descriptor;
67: val ACCOUNT_TYPE_USER = "FT4_USER";
69: namespace auth_flags {
79:     val ACCOUNT = "A";
87:     val TRANSFER = "T";
95: enum auth_type {
104: struct auth_descriptor {
114: entity account {
132: entity main_auth_descriptor {
144: entity account_auth_descriptor {
188: entity auth_descriptor_signer {
196: entity rl_state {
232: function add_auth_descriptor(account, auth_descriptor): account_auth_descriptor {
240:     val id = auth_descriptor.hash();
245:     val account_auth_descriptor = create account_auth_descriptor (
281: function update_main_auth_descriptor(account, auth_descriptor) {
288:     val account_auth_descriptor = add_auth_descriptor(account, auth_descriptor);
307: function delete_main_auth_descriptor(account) {
308:     val main_auth_descriptor = require(
313:     val auth_descriptor = main_auth_descriptor.auth_descriptor;
333: function set_main_auth_descriptor(account, account_auth_descriptor) {
355: function delete_auth_descriptor(auth_descriptor: account_auth_descriptor) {
373: function delete_all_auth_descriptors_except_main(account) {
374:     val main = main_auth_descriptor @ { account } ( .auth_descriptor );
409: function create_account_with_auth(auth_descriptor, account_id: byte_array? = null): account {
416:     val id = account_id ?: get_account_id_from_signers(get_signers(auth_descriptor));
417:     val account = create account(id, type = ACCOUNT_TYPE_USER);
419:     val main = add_auth_descriptor(account, auth_descriptor);
434: function require_mandatory_flags(auth_descriptor) {
435:     val flags = get_flags(auth_descriptor);
436:     val mandatory = get_auth_flags_config().mandatory;
452: function create_account_without_auth(account_id: byte_array, type: text): account {
470: function ensure_account_without_auth(account_id: byte_array, type: text): account {
471:     val account = account @? { .id == account_id };
489: function get_flags(auth_descriptor) {
502: function has_flags(account_auth_descriptor, required_flags: list<text>): boolean {
503:     val flags = get_flags_from_args(account_auth_descriptor.auth_type, account_auth_descriptor.args);
516: function add_signers(account, auth_descriptor, account_auth_descriptor) {
530: function get_account_id_from_signers(signers: list<byte_array>) {
543: function auth_descriptor_by_id(account, id: byte_array) = require(
555: function account_by_id(id: byte_array) = require(
566: function single_sig_auth_descriptor(signer: byte_array, flags: set<text>) = auth_descriptor(
583: function multi_sig_auth_descriptor(signers: list<byte_array>, signatures_required: integer, flags: set<text>) = auth_descriptor(
598: function get_auth_descriptors(id: byte_array) {
613: function get_auth_descriptors_by_signer(account_id: byte_array, signer: byte_array) {
628: function get_auth_descriptor_data(ad: struct<account_auth_descriptor>) {
648: function get_paginated_accounts_by_signer(
650:     val before_rowid = utils.before_rowid(page_cursor);
671: function get_paginated_accounts_by_ad_id(id: byte_array, page_size: integer?, page_cursor: text?) {
672:     val before_rowid = utils.before_rowid(page_cursor);
693: function get_paginated_accounts_by_type(type: text, page_size: integer?, page_cursor: text?) {
694:     val before_rowid = utils.before_rowid(page_cursor);
722: function validate_auth_descriptor_args(auth_descriptor) {
726:             val args = multi_sig_args.from_gtv(auth_descriptor.args.to_gtv());
740: function get_max_allowed_auth_descriptor_rules() = chain_context.args.auth_descriptor.max_rules;

### auth_flags_config.rell
File: rell/src/lib/ft4/core/accounts/auth_flags_config.rell

5: struct auth_flags_config {
17: function get_raw_auth_flags_config() {
22: function get_auth_flags_config() {
23:     val flags = chain_context.args.auth_flags;
24:     val mandatory = parse_auth_flags(flags.mandatory);
25:     val default = if (flags.default??) parse_auth_flags(flags.default!!) else mandatory;
45: function parse_auth_flags(gtv) {
69: function require_valid_auth_flags(flags: list<text>) {
70:     val invalid_flags = flags @* { not $.matches("[a-z_A-Z]+") };

### auth_basic.rell
File: rell/src/lib/ft4/core/accounts/auth_basic.rell

5: struct single_sig_args {
25: struct multi_sig_args {
68: function check_single_sig_auth(args: byte_array, signers: list<byte_array>, required_flags: list<text>): utils.validation_result {
76:     val ss_args = single_sig_args.from_bytes(args);
111: function check_multi_sig_auth(args: byte_array, signers: list<byte_array>, required_flags: list<text>): utils.validation_result {
112:     val multi_sign_args = multi_sig_args.from_bytes(args);
141: function check_required_flags(flags: set<text>, required_flags: list<text>): boolean {
151: function get_flags_from_args(a_t: auth_type, args: byte_array): set<text> {
176: function check_auth_args(a_t: auth_type, args: byte_array, signers: list<byte_array>, required_flags: list<text>): utils.validation_result {
191: function get_signers_from_encoded_auth_descriptor(a_t: auth_type, args: byte_array): list<byte_array> {
205: function get_signers(auth_descriptor)

### rate_limit.rell
File: rell/src/lib/ft4/core/accounts/rate_limit.rell

10: function account_rate_limit_config(account): rate_limit_config? = null;
27: function rate_limit(account) {
28:     val rl_config = get_rate_limit_config_for_account(account);
32:     val max_counter = rl_config.max_points;
33:     val recovery_time = rl_config.recovery_time;
34:     val stat = require(
38:     val delta = utils.latest_time() - stat.last_update;
68: function current_rate_limit_points(rate_limit_config, rl_state, current_timestamp: timestamp) {
69:     val delta = current_timestamp - rl_state.last_update;
92: function add_rate_limit_points(account, amount: integer) {
93:     val state = require(
97:     val config = get_rate_limit_config_for_account(account);
104: function get_rate_limit_config(): rate_limit_config = chain_context.args.rate_limit;
116: function get_rate_limit_config_for_account(account): rate_limit_config =
129: function create_rate_limiter_state_for_account(account) {
130:     val rl_config = get_rate_limit_config_for_account(account);
132:     val max_points = rl_config.max_points;
133:     val recovery_time = rl_config.recovery_time;
134:     val points_at_start = min(rl_config.points_at_account_creation, rl_config.max_points);

### linking/model.rell
File: rell/src/lib/ft4/core/accounts/linking/model.rell

31: entity account_link {

---
## 3. Admin (must not ship)

### core/admin/module.rell
File: rell/src/lib/ft4/core/admin/module.rell

10: struct module_args {
29: function require_admin() {
43: function get_admin_pubkey() {

### external/admin/operations.rell
File: rell/src/lib/ft4/external/admin/operations.rell

17: operation register_account(accounts.auth_descriptor) {
42: operation register_asset(name, symbol: text, decimals: integer, icon_url: text) {
71: operation register_asset_with_type(name, symbol: text, decimals: integer, icon_url: text, type: text) {
94: operation mint(account_id: byte_array, asset_id: byte_array, amount: big_integer) {
115: operation add_rate_limit_points(account_id: byte_array, amount: integer) {

### external/admin/crosschain/module.rell
File: rell/src/lib/ft4/external/admin/crosschain/module.rell

43: operation register_crosschain_asset(

core.admin require_admin comment says it is not intended for use in production and recommends a different framework so keys can be rotated and multisig can be used.

---
## 4. Auth module_args, handlers, ops

### core/auth/module.rell
File: rell/src/lib/ft4/core/auth/module.rell

14: struct module_args {
64: struct auth_data {
75: struct _auth_handler {
153: val APP_SCOPE = "app";
160: val OVERRIDE_PREFIX = "__override__";
175: function auth_handler(): map<name, _auth_handler>;
209: function get_auth_handler(op_name: name) {
211:     val auth_handlers = auth_handler();
213:     val override_name = OVERRIDE_PREFIX + op_name;
216:     val handler = get_mount_scope_auth_handler(auth_handlers, op_name);
255: function add_auth_handler(
282: function add_overridable_auth_handler(
300: function valid_scope_name(scope: text) {
301:     val scope_name = scope.trim();
311: function args() = op_context.get_current_operation().args.to_gtv();
314: struct signature {
328: struct evm_auth_args {
349: function get_mount_scope_auth_handler(auth_handlers: map<name, _auth_handler>, op_name: name) {
350:     val op_name_components = op_name.split(".");
354:         val mount_point = join_text_list(op_name_components, count);
373: function generate_operation_auth_message(blockchain_rid: byte_array, op: gtx_operation) {
380:             val argument_value = utils.convert_gtv_to_text(arg);
403: function join_text_list(components: list<text>, count: integer) {
422: function require_valid_scope_name(name) =
452: function is_evm_signatures_authorized_operation(name): boolean {
453:     val whitelisted_operations = set([
491: function require_evm_signatures_can_be_used() {
492:     val all_operations = op_context.get_all_operations();
541: function is_auth_blacklisted_operation(name): boolean {
542:     val blacklisted_operations = set([
574: function require_regular_next_operation() {
575:     val all_operations = op_context.get_all_operations();
576:     val this_op = op_context.get_all_operations()[op_context.op_index];
583:     val next_op = all_operations[op_context.op_index + 1];

### core/auth/login.rell
File: rell/src/lib/ft4/core/auth/login.rell

2: val DEFAULT_LOGIN_CONFIG_NAME = "default";
19: struct _login_config {
49: function login_config(): map<name, _login_config>;
66: function add_login_config(
82: function map_rule(rule: rule_expression): gtv {
95: function login_simple_rule(rule: rule_expression) = map_rule(rule);
107: function login_rules(rules_list: list<rule_expression>): gtv {
112:     val rules = ["and".to_gtv()];
125: function ttl(millis: integer): gtv {
153: enum rule_variable {
173: enum rule_operator {
182: struct rule_expression {
189: struct rule_parameters {
199: function greater_than(rule_parameters) = rule_expression(
206: function greater_or_equal(rule_parameters) = rule_expression(
213: function equals(rule_parameters): rule_expression {
222: function less_than(rule_parameters) = rule_expression(
229: function less_or_equal(rule_parameters) = rule_expression(
240: function block_height(integer) = rule_parameters(
246: function block_time(integer) = rule_parameters(
252: function op_count(integer) = rule_parameters(
264: function relative_block_height(integer) = rule_parameters(
276: function relative_block_time(integer) = rule_parameters(

### core/auth/authentication.rell
File: rell/src/lib/ft4/core/auth/authentication.rell

11: function before_authenticate(accounts.account, accounts.account_auth_descriptor);
23: function after_authenticate(accounts.account, accounts.account_auth_descriptor?);
26: val EVM_AUTH_OP = "ft4.evm_auth";
28: val FT_AUTH_OP = "ft4.ft_auth";
31: val EVM_ADDRESS_SIZE = 20;
33: val FT_PUBKEY_SIZE = 33;
36: val BLOCKCHAIN_RID_PLACEHOLDER = "{blockchain_rid}";
40: val ACCOUNT_ID_PLACEHOLDER = "{account_id}";
45: val AUTH_DESCRIPTOR_ID_PLACEHOLDER = "{auth_descriptor_id}";
47: val NONCE_PLACEHOLDER = "{nonce}";
110: function authenticate_and_return_context()  {
111:     val op = op_context.get_current_operation();
117:     val previous_op = op_context.get_all_operations()[op_context.op_index - 1];
124:     val (account, auth_descriptor) = fetch_account_and_auth_descriptor(previous_op.args);
143:     val flags = get_auth_flags(op.name);
152:     val auth_desc_after = accounts.update_auth_descriptor_rule_variables(auth_descriptor);
153:     val deleted_descriptors = accounts.delete_expired_auth_descriptors(account, auth_desc_after);
164: function authenticate(){
165:     val (account, auth_desc_after, deleted_descriptors) = authenticate_and_return_context();
186: function get_first_allowed_auth_descriptor_by_signers(op_name: name, args: gtv, account_id: byte_array, signers: list<byte_array>) {
187:     val ads = accounts.auth_descriptor_signer @* {
192:     val flags = get_auth_flags(op_name);
194:     val valid_ad_ids = ads @* { accounts.has_flags($, flags) } (.id);
221: function get_first_allowed_auth_descriptor(op_name: name, args: gtv, account_id: byte_array, ad_ids: list<byte_array>) {
224:     val resolver = get_auth_handler(op_name).resolver;
241: function fetch_account_and_auth_descriptor(auth_args: list<gtv>): (accounts.account, accounts.account_auth_descriptor) {
242:     val (account_id, auth_descriptor_id) = extract_account_and_auth_descriptor(auth_args);
243:     val account = accounts.Account(account_id);
244:     val auth_descriptor = require(
260: function extract_account_and_auth_descriptor(auth_args: list<gtv>): (byte_array, byte_array) {
261:     val account_id = byte_array.from_gtv(auth_args[0]);
262:     val auth_descriptor_id = byte_array.from_gtv(auth_args[1]);
273: function try_fetch_auth_descriptor(accounts.account, auth_descriptor_id: byte_array): accounts.account_auth_descriptor? =
285: function get_auth_flags(op_name: name) = get_auth_handler(op_name).flags;
297: function get_auth_message_template(op_name: name, op_args: gtv?) {
298:     val formatter = get_auth_handler(op_name).message_formatter;
300:     val args = if (op_args == null) list<gtv>() else list<gtv>.from_gtv(op_args);
302:     val message = if (formatter??) formatter(args.to_gtv()) else generate_operation_auth_message(chain_context.blockchain_rid, gtx_operation(
328: function _validate_evm_signature(
335:     val message_template = get_auth_message_template(op.name, op.args.to_gtv());
336:     val validated_args = _validate_evm_arguments(auth_op.args, set(flags), account, auth_descriptor);
337:     val message = create_message_from_template(
362: function create_message_from_template(evm_auth_args, message_template: text, args: list<gtv>, nonce: text): text {
398: function _validate_evm_arguments(
409:     val signatures = list<signature?>.from_gtv(auth_args[2]);
411:     val has_all_flags = accounts.has_flags(auth_descriptor, list(required_flags));
437: function _validate_ft4_signature(
442:    	val result: utils.validation_result = accounts.check_auth_args(
460: function _recover_evm_address(message: text, signature) {
463:     val msg_hash = _evm_message_hash(message);
464:     val evm_pubkey = crypto.eth_ecrecover(
482: function _evm_message_hash(message: text): byte_array =
503: function _validate_evm_address(message: text, signature?, accounts.account_auth_descriptor) {
508:     val recovered_address = _recover_evm_address(message, signature);
509:     val signer = accounts.auth_descriptor_signer @? {
541: function _validate_multiple_evm_addresses(message: text, signatures: list<signature?>, accounts.account_auth_descriptor) {
542:     val recovered_keys = list<byte_array>();
543:     val ad_args = accounts.multi_sig_args.from_bytes(account_auth_descriptor.args);
547:         val recovered_address = _validate_evm_address(
594: function verify_signers(ft_and_evm_signers: list<byte_array>) {
595:     val op = op_context.get_current_operation();
596:     val message_template = get_auth_message_template(op.name, op.args.to_gtv());
603:         val auth_details = require(
643: function verify_signers_with_message(ft_and_evm_signers: list<byte_array>, message: text) {
644:     val (signers, signatures) = get_evm_signatures();
646:         val signer = signers[i];
653:     val evm_signers = set(signers);
683: function validate_signer(signer: byte_array) {
701: function get_evm_signatures(): (signers: list<byte_array>, signatures: list<signature>) {
704:     val tx_operations = op_context.get_all_operations();
705:     val prev_op = tx_operations[op_context.op_index-1];
720:     val args = struct<evm_signatures>.from_gtv(evm_signatures_op.args.to_gtv());
729:         val signature = require(
752: function extract_account_id(auth_op: gtx_operation): byte_array {
772: function is_auth_op(op: gtx_operation) = op.name in [EVM_AUTH_OP, FT_AUTH_OP];
781: function is_evm_signatures_op(op: gtx_operation) = op.name == "ft4.evm_signatures";
790: function get_auth_details_from_auth_operation() {
792:     val op = op_context.get_all_operations()[op_context.op_index-1];

### external/auth/operations.rell
File: rell/src/lib/ft4/external/auth/operations.rell

40: operation evm_auth(
76: operation evm_signatures(
101: operation ft_auth(

### external/auth/queries.rell
File: rell/src/lib/ft4/external/auth/queries.rell

6: struct _auth_handler_client {
33: query get_auth_flags(op_name: name) = auth.get_auth_handler(op_name).flags;
45: query get_auth_message_template(op_name: name, op_args: gtv?) = auth.get_auth_message_template(op_name, op_args);
57: query get_login_config(name? = null) {
58:     val configs = auth.login_config();
59:     val config_name = name ?: auth.DEFAULT_LOGIN_CONFIG_NAME;
86: query get_auth_handler_for_operation(op_name: name) {
87:   val handler = auth.get_auth_handler(op_name);
108: query get_first_allowed_auth_descriptor_by_signers(
128: query get_first_allowed_auth_descriptor(
140: query get_all_auth_handlers() {
141:   val auth_handlers = auth.auth_handler();

---
## 5. External accounts and assets

### external/accounts/operations.rell
File: rell/src/lib/ft4/external/accounts/operations.rell

2: function delete_auth_descriptor_message(gtv) {
3:     val params = struct<delete_auth_descriptor>.from_gtv(gtv);
7: function delete_auth_descriptor_resolver(args: gtv, account_id: byte_array, auth_descriptor_ids: list<byte_array>) {
8:     val params = struct<delete_auth_descriptor>.from_gtv(args);
19: function () = auth.add_overridable_auth_handler(
42: operation delete_auth_descriptor(auth_descriptor_id: byte_array) {
43:     val (account, auth_desc_after, deleted_descriptors)  = auth.authenticate_and_return_context();
44:     val descriptor_is_deleted = deleted_descriptors @? {auth_descriptor_id==.id}(.id) != null;
51: function delete_auth_descriptors_for_signer_message(gtv) {
52:     val params = struct<delete_auth_descriptors_for_signer>.from_gtv(gtv);
56: function delete_auth_descriptors_for_signer_resolver(args: gtv, account_id: byte_array, auth_descriptor_ids: list<byte_array>) {
57:     val params = struct<delete_auth_descriptors_for_signer>.from_gtv(args);
59:     val ads = (a_ad: accounts.account_auth_descriptor, ad_s: accounts.auth_descriptor_signer) @* {
78: function () = auth.add_overridable_auth_handler(
103: operation delete_auth_descriptors_for_signer(signer: byte_array) {
104:     val account = auth.authenticate();
108:     val ads = (a_ad: accounts.account_auth_descriptor, ad_s: accounts.auth_descriptor_signer) @* {
118: function delete_all_auth_descriptors_except_main_message(gtv) {
122: function delete_all_auth_descriptors_except_main_resolver(args: gtv, account_id: byte_array, auth_descriptor_ids: list<byte_array>) {
123:     val main_auth_descriptor = require(
132: function () = auth.add_overridable_auth_handler(
149: operation delete_all_auth_descriptors_except_main() {
150:     val account = auth.authenticate();
154: function add_auth_descriptor_message(gtv) {
155:     val params = struct<add_auth_descriptor>.from_gtv(gtv);
156:     val flags = params.new_desc.args[0];
162: function () = auth.add_overridable_auth_handler(
181: operation add_auth_descriptor(new_desc: accounts.auth_descriptor) {
182:     val account = auth.authenticate();
183:     val signers = accounts.get_signers(new_desc);
188: function update_main_auth_descriptor_message(gtv) {
189:     val params = struct<update_main_auth_descriptor>.from_gtv(gtv);
190:     val flags = params.new_desc.args[0];
191:     val signers = accounts.get_signers(params.new_desc);
196: function update_main_auth_descriptor_resolver(args: gtv, account_id: byte_array, auth_descriptor_ids: list<byte_array>) {
197:     val main_auth_descriptor = require(
206: function () = auth.add_overridable_auth_handler(
231: operation update_main_auth_descriptor(new_desc: accounts.auth_descriptor) {
232:     val account = auth.authenticate();

### external/accounts/queries.rell
File: rell/src/lib/ft4/external/accounts/queries.rell

11: query get_accounts_filtered(account_filter: account_filter, page_size: integer?, page_cursor: text?) {
12:     val before_rowid = utils.before_rowid(page_cursor);
37: query get_account_auth_descriptors_filtered(account_auth_descriptor_filter?, page_size: integer?, page_cursor: text?) {
38:     val before_rowid = utils.before_rowid(page_cursor);
63: query get_main_auth_descriptors_filtered(
68:     val before_rowid = utils.before_rowid(page_cursor);
93: query get_auth_descriptor_signers_filtered(auth_descriptor_signer_filter?, page_size: integer?, page_cursor: text?) {
94:     val before_rowid = utils.before_rowid(page_cursor);
119: query get_rl_states_filtered(rl_state_filter?, page_size: integer?, page_cursor: text?) {
120:     val before_rowid = utils.before_rowid(page_cursor);
139: query get_config() {
160: query get_account_rate_limit_last_update(account_id: byte_array) {
161:     val account = accounts.Account(account_id);
182: query is_auth_descriptor_valid(account_id: byte_array, auth_descriptor_id: byte_array) {
202: query get_account_auth_descriptors(id: byte_array) {
216: query get_account_auth_descriptors_by_signer(account_id: byte_array, signer: byte_array) {
230: query get_account_auth_descriptor_by_id(account_id: byte_array, id: byte_array) {
248: query get_account_main_auth_descriptor(account_id: byte_array) {
260: query get_account_by_id(id: byte_array) {
276: query get_accounts_by_signer(id: byte_array, page_size: integer?, page_cursor: text?) {
294: query get_accounts_by_auth_descriptor_id(id: byte_array, page_size: integer?, page_cursor: text?) {
311: query get_accounts_by_type(type: text, page_size: integer, page_cursor: text?) {
326: query get_auth_descriptor_counter(

### external/assets/operations.rell
File: rell/src/lib/ft4/external/assets/operations.rell

2: function transfer_message(gtv) {
3:     val params = struct<transfer>.from_gtv(gtv);
4:     val asset = assets.Asset(params.asset_id);
16: function () = auth.add_overridable_auth_handler(
42: operation transfer(recipient_id: byte_array, asset_id: byte_array, amount: big_integer) {
43:     val sender = auth.authenticate();
44:     val asset = assets.Asset(asset_id);
48: function recall_unclaimed_transfer_message(gtv) {
49:     val params = struct<recall_unclaimed_transfer>.from_gtv(gtv);
58: function () = auth.add_overridable_auth_handler(
80: operation recall_unclaimed_transfer(transfer_tx_rid: byte_array, transfer_op_index: integer) {
81:     val account = auth.authenticate();
86: function () = auth.add_overridable_auth_handler(
92: function burn_message(gtv) {
93:     val params = struct<burn>.from_gtv(gtv);
94:     val asset = assets.Asset(params.asset_id);
118: operation burn(asset_id: byte_array, amount: big_integer) {
119:     val account = auth.authenticate();

### external/assets/queries.rell
File: rell/src/lib/ft4/external/assets/queries.rell

10: query get_asset_balances(account_id: byte_array, page_size: integer?, page_cursor: text?) {
24: query get_asset_balance(account_id: byte_array, asset_id: byte_array) {
40: query get_assets_by_name(name, page_size: integer?, page_cursor: text?) {
56: query get_assets_by_symbol(symbol: text, page_size: integer?, page_cursor: text?) {
68: query get_asset_by_id(asset_id: byte_array) {
79: query get_assets_filtered(asset_filter: assets.asset_filter?, page_size: integer?, page_cursor: text?) {
93: query get_balances_filtered(balance_filter: assets.balance_filter?, page_size: integer?, page_cursor: text?) {
107: query get_transfer_history_entries_filtered(
129: query get_crosschain_transfer_history_entries_filtered(
153: query get_assets_by_type(type: text, page_size: integer?, page_cursor: text?) {
168: query get_all_assets(page_size: integer?, page_cursor: text?) {
183: query get_asset_details_for_crosschain_registration(asset_id: byte_array) {
198: query get_transfer_history(account_id: byte_array, filter: assets.filter, page_size: integer?, page_cursor: text?) {
199:     val account = accounts.account @? { account_id };
202:     val paginated_transfers = assets.get_paginated_transfers(
222: query get_transfer_history_from_height(height: integer, asset_id: byte_array?, page_size: integer?, page_cursor: text?) {
223:     val asset = if (asset_id != null) assets.Asset(asset_id) else null;
225:     val paginated_transfers = assets.get_paginated_transfers(
245: query get_transfer_history_entry(rowid) {
246:     val entry = assets.transfer_history_entry @? { .rowid == rowid };
257: query get_transfer_details(tx_rid: byte_array, op_index: integer): list<assets.transfer_detail> =
268: query get_transfer_details_by_asset(tx_rid: byte_array, op_index: integer, asset_id: byte_array): list<assets.transfer_detail> =

### core/assets/asset.rell
File: rell/src/lib/ft4/core/assets/asset.rell

5: val ASSET_TYPE_FT4 = "ft4";
11: entity asset {
79: function Asset(id: byte_array) = require(
87: entity balance {
104: val max_asset_amount = big_integer.from_hex("ff".repeat(32));
121: function increase_balance(accounts.account, asset, amount: big_integer): big_integer {
122:     val balance = balance @? { account, asset };
124:         val new_amount = balance.amount + amount;
152: function deduct_balance(accounts.account, asset, amount: big_integer): big_integer {
153:     val balance = balance @? { account, asset };
161:        val new_amount = balance.amount - amount;
173: function get_asset_balance(accounts.account, asset): big_integer {
174:     val balance = balance @? {asset, account};
187: function get_paginated_asset_balances(account_id: byte_array, page_size: integer?, page_cursor: text?): list<utils.pagination_result> {
188:     val before_rowid = utils.before_rowid(page_cursor);
212: function get_assets_by_type(type: text, page_size: integer?, page_cursor: text?) {
213:     val before_rowid = utils.before_rowid(page_cursor);
233: function get_all_assets(page_size: integer?, page_cursor: text?) {
234:     val before_rowid = utils.before_rowid(page_cursor);
250: function get_asset_balances(account_id: byte_array) {
268: function format_amount_with_decimals(amount: big_integer, decimals: integer): text {
293: function get_paginated_assets_by_name(name, page_size: integer?, page_cursor: text?) {
294:     val before_rowid = utils.before_rowid(page_cursor);
315: function get_paginated_assets_by_symbol(symbol: text, page_size: integer?, page_cursor: text?) {
316:     val before_rowid = utils.before_rowid(page_cursor);
336: function get_asset_details_for_crosschain_registration(asset_id: byte_array) {
337:     val asset = asset @? { .id == asset_id } (
368: function require_zero_exclusive_asset_amount_limits(value: big_integer, name) {
390: function parse_icon_url(icon_url: text): text {
391:     val trimmed_icon_url = icon_url.trim();
406: function validate_asset_decimals(decimals: big_integer) {
420: function validate_asset_id(id: byte_array) {
434: function validate_asset_name(name) {
448: function validate_asset_symbol(symbol: text) {
462: function validate_asset_type(type: text) {
474: function validate_asset_uniqueness_resolver(res: byte_array) {
481: namespace Unsafe {
505:     function mint(accounts.account, asset, amount: big_integer) {
556:     function burn(accounts.account, asset, amount: big_integer) {
605:     function register_asset(
619:         val id = (name, blockchain_rid).hash();

### core/assets/transfer.rell
File: rell/src/lib/ft4/core/assets/transfer.rell

13: function is_create_on_internal_transfer_enabled(): boolean = false;
28: function create_on_internal_transfer(
47: function recall_on_internal_transfer(
53: namespace Unsafe {
78:    function transfer_to_recipient_id(sender: accounts.account, recipient_id: byte_array, asset, amount: big_integer) {
80:       val recipient = accounts.account @? { .id == recipient_id };
109:    function transfer(from: accounts.account, to: accounts.account, asset, amount: big_integer) {
145:    function recall_unclaimed_transfer(accounts.account, transfer_tx_rid: byte_array, transfer_op_index: integer) {

### core/assets/locking/functions.rell
File: rell/src/lib/ft4/core/assets/locking/functions.rell

20: val ACCOUNT_TYPE_LOCK = "FT4_LOCK";
33: function ensure_lock_account(type: text, accounts.account): accounts.account {
34:     val link = linking.account_link @? {
42:     val secondary = accounts.create_account_without_auth(
63: function get_lock_accounts(accounts.account) {
80: function get_lock_accounts_with_non_zero_balances(accounts.account) {
99: function get_lock_account_id(accounts.account, type: text) {
121: function lock_asset(type: text, accounts.account, assets.asset, amount: big_integer) {
122:     val lock_account = ensure_lock_account(type, account);
148: function unlock_asset(type: text, accounts.account, assets.asset, amount: big_integer) {
149:     val lock_account = ensure_lock_account(type, account);
177: function get_locked_asset_balance(
184:     val before_rowid = utils.before_rowid(page_cursor);
224: function get_locked_asset_aggregated_balance(
262: function get_locked_asset_balances(
268:     val before_rowid = utils.before_rowid(page_cursor);
269:     val assets = (al: linking.account_link, b: assets.balance) @* {
312: function get_locked_asset_aggregated_balances(
318:     val before_rowid = utils.before_rowid(page_cursor);
319:     val balances = (al: linking.account_link, b: assets.balance) @* {

---
## 6. Strategies

### strategies/module.rell
File: rell/src/lib/ft4/core/accounts/strategies/module.rell

67: function strategy(): map<name, _strategy>;
103: struct _strategy {
128: struct account_details {
191: function add_strategy(
222: function _no_action(accounts.account, strategy_params: gtv) {}
233: function _default_signers(account_details: (gtv) -> account_details, gtv) {
234:     val details = account_details(gtv);
235:     val default_signers = accounts.get_signers(details.main);
255: function get_strategy(strategy_name: name): _strategy {
256:     val strategies = strategy();
271: function is_strategy_op(op: gtx_operation): boolean {
272:     val strategy_operations = strategy().values() @* {} ($.op.mount_name);
326: function register_account(): accounts.account {
327:     val tx_operations = op_context.get_all_operations();
328:     val strategy_op = tx_operations[op_context.op_index-1];
329:     val strategy_name = strategy_op.name;
330:     val strategy_params = strategy_op.args.to_gtv();
332:     val strategy = get_strategy(strategy_name);
333:     val account_details = strategy.account_details(strategy_params);
334:     val signers = strategy.required_signers(strategy_params);
336:     val message = get_register_account_message(strategy_op, op_context.get_current_operation());
339:     val account_id = account_details.account_id;
343:     val account = accounts.create_account_with_auth(account_details.main, account_id);
370: function require_register_account_next_operation() {
371:     val all_operations = op_context.get_all_operations();
372:     val this_op = op_context.get_all_operations()[op_context.op_index];
380:     val next_op = all_operations[op_context.op_index + 1];
395: function get_account_id_for_strategy(strategy_op: gtx_operation) {
396:     val strategy = get_strategy(strategy_op.name);

### strategies/open
File: rell/src/lib/ft4/core/accounts/strategies/open/module.rell

22: operation ras_open(
35: function account_details(gtv) {
36:     val params = struct<ras_open>.from_gtv(gtv);
37:     val signers = accounts.get_signers(params.main);
46: function () = strategies.add_strategy(

### strategies/transfer
File: rell/src/lib/ft4/core/accounts/strategies/transfer/module.rell

17: val ACCOUNT_TYPE_POOL = "FT4_POOL";
26: val ACCOUNT_TYPE_FEE = "FT4_FEE";
34: enum pending_transfer_expiration_state {
44: struct pending_transfer_filter {
62: struct module_args_list_element {
155: struct module_args {
190: enum account_creation_state {
203: entity account_creation_transfer {
283:     val sender_blockchain_rid = chain_context.blockchain_rid; // internal transfer
302:     val sender_blockchain_rid = chain_context.blockchain_rid; // internal transfer
303:     val (asset, amount) = recall_transfer(sender_blockchain_rid, sender.id, transfer_tx_rid, transfer_op_index);
352:     val (asset, amount) = recall_transfer(sender_blockchain_rid, sender_id, transfer_tx_rid, transfer_op_index);
384: function pool_assets(
390:     val rules = find_allowed_rules(
431: function pool_assets_with_rules(
476: function collect_pooled_assets(accounts.account, recipient_id: byte_array) {
483:         val pool_account = ensure_chain_pool_account();
517: function do_transfer(accounts.account, strategy: text) {
518:     val account_creation_transfers = account_creation_transfer @* {
527:     val pool_account = ensure_chain_pool_account();
541:         val rules = map<text,rule>.from_gtv(gtv.from_bytes(account_creation_transfer.rules));
542:         val rule = rules.get_or_null(strategy);
544:             val resolved_assets = resolve_allowed_assets(rule);
572: function has_create_transfer_timed_out(account_creation_transfer): boolean {
573:     val rules = map<text,rule>.from_gtv(gtv.from_bytes(account_creation_transfer.rules));
574:     val timeout = (rules.values() @ {} (@min .timeout_days))!! * utils.MILLISECONDS_PER_DAY;
575:     val occurred_time = account_creation_transfer.timestamp;
576:     val delta = (utils.latest_time() - occurred_time);
589: function filter_account_creation_transfer(
624: function recall_transfer(
630:     val account_creation_transfers = account_creation_transfer @* {
644:     val account_creation_transfer = account_creation_transfers[0];
669: function ensure_pool_account(blockchain_rid: byte_array): accounts.account {
670:     val pool_account_id = (ACCOUNT_TYPE_POOL + chain_context.blockchain_rid).hash();
679: function ensure_chain_pool_account(): accounts.account {
698: function ensure_fee_account(custom_account_id: byte_array?, blockchain_rid: byte_array): accounts.account {
702:         val fee_account_id = (ACCOUNT_TYPE_FEE + chain_context.blockchain_rid).hash();
717: function ensure_chain_fee_account(custom_account_id: byte_array?): accounts.account {
739: function required_signers(main_auth_descriptor: accounts.auth_descriptor, disposable_auth_descriptor: accounts.auth_descriptor? = null) {
740:     val main_signers = accounts.get_signers(main_auth_descriptor);
749:     val recipient_id = accounts.get_account_id_from_signers(main_signers);
751:     val account_creation_transfers = account_creation_transfer @* {

### strategies/transfer/config_utils
File: rell/src/lib/ft4/core/accounts/strategies/transfer/config_utils.rell

2: val _CONFIG_ERROR_TEXT = "CONFIG ERROR: Error in moduleArgs (lib.ft4.core.accounts.strategy.transfer): ";
9: val CURRENT_CHAIN_REF = "$";
17: val CURRENT_ACCOUNT_REF = "X";
26: val ANY_REF = "*";
32: struct allowlist {
52: struct asset_limit {
70: struct rule {
126: function get_config(config_gtv: gtv = chain_context.args.rules): list<rule> {
127:     val config = list<rule>();
129:     val inputs = list<module_args_list_element>.from_gtv_pretty(config_gtv);
132:         val strats = if (utils.is_list(input.strategy))
136:         val sender_res = parse_account_ids_from_gtv(
140:         val recipient_res = parse_account_ids_from_gtv(
152:         val res = parse_asset_limits_from_gtv(input.asset);
188: function parse_account_ids_from_gtv(gtv): (
193:         val allowed_values = list<byte_array>();
194:         val gtv_list = list<gtv>.from_gtv(gtv);
197:             val parsed_element = parse_account_id_single_value_from_gtv(element);
213:         val parsed_gtv = parse_account_id_single_value_from_gtv(gtv);
231: function parse_account_id_single_value_from_gtv(gtv): (
241:         val input = text.from_gtv(gtv);
283: function parse_blockchains_from_gtv(gtv): allowlist {
285:         val allowed_values = list<byte_array>();
286:         val gtv_list = list<gtv>.from_gtv(gtv);
289:             val parsed_element = parse_blockchain_single_value_from_gtv(element);
301:         val parsed_gtv = parse_blockchain_single_value_from_gtv(gtv);
316: function parse_blockchain_single_value_from_gtv(gtv): (
324:         val input = text.from_gtv(gtv);
364: function parse_asset_limits_from_gtv(gtv): list<asset_limit>? {
366:         val input = map<name, gtv>.from_gtv_pretty(gtv);
369:         val inputs = list<map<name, gtv>>.from_gtv_pretty(gtv);
390: function parse_asset_limit(input: map<name, gtv>): asset_limit {
408: function parse_amount(input: gtv): big_integer = if (utils.is_text(input))

### strategies/transfer/fee_config
File: rell/src/lib/ft4/core/accounts/strategies/transfer/fee_config.rell

2: val _CONFIG_ERROR_TEXT_TEMPLATE = "CONFIG ERROR: Error in moduleArgs (%s): %s";
16: struct fee_asset {
49: function resolve_fee_assets(config: list<fee_asset>): map<byte_array, big_integer> {
50:     val fee_assets = map<byte_array, big_integer>();
52:         val asset_id = fee_asset.id ?:
73: function parse_fee_assets(gtv, module_name: text): list<fee_asset> {
75:         val input = map<name, gtv>.from_gtv_pretty(gtv);
78:         val inputs = list<map<name, gtv>>.from_gtv_pretty(gtv);
103: function parse_fee_asset(input: map<name, gtv>, module_name: text): fee_asset {

### strategies/transfer/fee
File: rell/src/lib/ft4/core/accounts/strategies/transfer/fee/module.rell

23: operation ras_transfer_fee(
37: function account_details(gtv) {
38:     val params = struct<ras_transfer_fee>.from_gtv(gtv);
39:     val signers = accounts.get_signers(params.main);
61: function transfer_action(accounts.account, strategy_params_gtv: gtv) {
64: 	val strategy_params = struct<ras_transfer_fee>.from_gtv(strategy_params_gtv);
65:     val fee_assets = transfer.resolve_fee_assets(fee_assets());
66: 	val fee_amount = require(
71:     val asset = assets.Asset(strategy_params.asset_id);
80: function () = strategies.add_strategy(
95: function required_signers_fee(gtv) {
96:     val params = struct<ras_transfer_fee>.from_gtv(gtv);

### strategies/transfer/fee/config
File: rell/src/lib/ft4/core/accounts/strategies/transfer/fee/config.rell

2: val MODULE_NAME = "lib.ft4.core.accounts.strategy.transfer.fee";
5: struct module_args {
20: function fee_assets(): list<transfer.fee_asset> = transfer.parse_fee_assets(chain_context.args.asset, MODULE_NAME);
32: function fee_account_id(): byte_array? = chain_context.args.fee_account;

### strategies/transfer/open
File: rell/src/lib/ft4/core/accounts/strategies/transfer/open/module.rell

25: operation ras_transfer_open(
38: function account_details(gtv) {
39:     val params = struct<ras_transfer_open>.from_gtv(gtv);
40:     val signers = accounts.get_signers(params.main);
55: function transfer_action(accounts.account, strategy_params_gtv: gtv) {
60: function () = strategies.add_strategy(
75: function required_signers_open(gtv) {
76:     val params = struct<ras_transfer_open>.from_gtv(gtv);

### strategies/transfer/subscription
File: rell/src/lib/ft4/core/accounts/strategies/transfer/subscription/module.rell

14: entity subscription {
40: operation ras_transfer_subscription(
54: function account_details(gtv) {
55:     val params = struct<ras_transfer_subscription>.from_gtv(gtv);
56:     val signers = accounts.get_signers(params.main);
81: function transfer_action(accounts.account, strategy_params_gtv: gtv) {
84: 	val strategy_params = struct<ras_transfer_subscription>.from_gtv(strategy_params_gtv);
85:     val subscription_assets = transfer.resolve_fee_assets(subscription_assets());
86: 	val subscription_amount = require(
91:     val asset = assets.Asset(strategy_params.asset_id);
107: function () = strategies.add_strategy(
122: function required_signers_subscription(gtv) {
123:     val params = struct<ras_transfer_subscription>.from_gtv(gtv);
128: function () = auth.add_overridable_auth_handler(
150: operation renew_subscription(asset_id: byte_array?) {
151:     val account = auth.authenticate();
153:     val subscription = require(subscription @? { account },
156:     val subscription_assets = transfer.resolve_fee_assets(subscription_assets());
157:     val subscription_asset = if (asset_id != null) assets.Asset(asset_id) else subscription.asset;
158:     val subscription_amount = require(subscription_assets.get_or_null(subscription_asset.id),
167:     val remaining_period = max(0, subscription.last_payment + subscription.period_millis - utils.latest_time());
173: function renew_subscription_message(gtv) {
192:     val subscription = subscription @? { account };
194:         val op = op_context.get_current_operation();

### strategies/transfer/subscription/config
File: rell/src/lib/ft4/core/accounts/strategies/transfer/subscription/config.rell

2: val MODULE_NAME = "lib.ft4.core.accounts.strategy.transfer.subscription";
5: struct module_args {
31: function subscription_assets(): list<transfer.fee_asset> =
35: function subscription_period_days(): integer = chain_context.args.subscription_period_days;
38: function free_operations(): set<text> {
39:     val ops = set<text>.from_gtv(chain_context.args.free_operations);
54: function subscription_account_id(): byte_array? = chain_context.args.subscription_account;

### external/strategies/operations
File: rell/src/lib/ft4/external/accounts/strategies/operations.rell

42: operation register_account() {

---
## 7. Crosschain

### core/crosschain/module.rell
File: rell/src/lib/ft4/core/crosschain/module.rell

8: val MAX_PATH_LENGTH = 100;
20: function is_create_on_crosschain_transfer_enabled(): boolean = false;
36: function create_on_crosschain_transfer(
57: function recall_on_crosschain_transfer(
78: function get_paginated_pending_transfers(
83:     val after_rowid = utils.before_rowid(page_cursor);
112: function get_last_pending_transfer_for_account(

### core/crosschain/transfer.rell
File: rell/src/lib/ft4/core/crosschain/transfer.rell

23: entity applied_transfers {
48: entity canceled_transfers {
68: entity unapplied_transfers {
99: entity recalled_transfers {
116: entity pending_transfer {
140: entity reverted_transfer {
148: namespace Unsafe {
179:     function update_balances_if_needed(
202:         val origin_blockchain_rid = asset_origin @? { asset } .origin_blockchain_rid;
203:         val is_sender_blockchain = sender.type == ACCOUNT_TYPE_BLOCKCHAIN;
204:         val is_recipient_blockchain = recipient.type == ACCOUNT_TYPE_BLOCKCHAIN;
206:         val change_sender = not exists(origin_blockchain_rid) or not is_sender_blockchain or origin_blockchain_rid != sender.id;
207:         val change_recipient = not exists(origin_blockchain_rid) or not is_recipient_blockchain or origin_blockchain_rid != recipient.id;

### core/crosschain/blockchain.rell
File: rell/src/lib/ft4/core/crosschain/blockchain.rell

17: val ACCOUNT_TYPE_BLOCKCHAIN = "FT4_BLOCKCHAIN";
42: entity asset_origin {
54: function is_blockchain(account_id: byte_array): boolean {
67: function ensure_blockchain_account(blockchain_rid: byte_array) =
70: namespace Unsafe {
110:     function register_crosschain_asset(
140:         val asset = create assets.asset(

### external/crosschain/operations.rell
File: rell/src/lib/ft4/external/crosschain/operations.rell

2: function init_transfer_message(gtv) {
3:     val params = struct<init_transfer>.from_gtv(gtv);
4:     val asset = assets.Asset(params.asset_id);
17: function () = auth.add_overridable_auth_handler(
88: operation init_transfer(
102:     val account = auth.authenticate();
103:     val asset = require(
140:     val pending_transfer = create crosschain.pending_transfer(
244: operation apply_transfer(
251:     val (init_transfer_details, from_blockchain_rid) = validate_apply_transfer(
265:     val asset = assets.Asset(init_transfer_details.args.asset_id);
267:     val is_intermediate = init_transfer_details.args.hops.size() > hop_index + 1;
281:         val next_hop = init_transfer_details.args.hops[hop_index + 1];
290:         val recipient = accounts.account @? { .id == init_transfer_details.args.recipient_id };
321:     val applied_transfer = create crosschain.applied_transfers(
368: operation complete_transfer(
378:     val final_apply_transfer_args = struct<apply_transfer>.from_gtv(final_apply_transfer_tx.body.operations[op_index].args.to_gtv());
381:     val init_transfer_tx_hash = final_apply_transfer_args.init_transfer_tx.to_gtv().hash();
382:     val init_transfer_args = struct<init_transfer>.from_gtv(final_apply_transfer_args.init_transfer_tx.body.operations[final_apply_transfer_args.init_tx_op_index].args.to_gtv());
446: operation cancel_transfer(
453:     val (init_transfer_details, from_blockchain_rid) = validate_apply_transfer(
535: operation unapply_transfer(
542:     val init_transfer_details = get_init_transfer_details(init_transfer_tx, init_tx_op_index);
544:     val (apply_tx_data, apply_op_index) = require_not_empty(
562:     val last_op = last_tx.body.operations[last_op_index];
566:             val args = struct<cancel_transfer>.from_gtv(last_op.args.to_gtv());
585:             val args = struct<unapply_transfer>.from_gtv(last_op.args.to_gtv());
596:             val args = struct<recall_unclaimed_transfer>.from_gtv(last_op.args.to_gtv());
625:     val current_hop_blockchain_rid = init_transfer_details.args.hops[hop_index];
631:     val next_hop_blockchain_rid = init_transfer_details.args.hops[hop_index + 1];
652:     val asset = assets.Asset(init_transfer_details.args.asset_id);
653:     val previous_hop = if (hop_index > 0) init_transfer_details.args.hops[hop_index - 1] else init_transfer_tx.body.blockchain_rid;
720: operation revert_transfer(
726:     val init_transfer_details = get_init_transfer_details(init_transfer_tx, init_tx_op_index);
727:     val hops = init_transfer_details.args.hops;
729:     val account = accounts.Account(init_transfer_details.sender_account_id);
730:     val asset = assets.Asset(init_transfer_details.args.asset_id);
731:     val amount = init_transfer_details.args.amount;
733:     val next_hop_blockchain_rid = hops[0];
748:     val last_op = last_tx.body.operations[last_op_index];
752:             val args = struct<cancel_transfer>.from_gtv(last_op.args.to_gtv());
763:             val args = struct<unapply_transfer>.from_gtv(last_op.args.to_gtv());
774:             val args = struct<recall_unclaimed_transfer>.from_gtv(last_op.args.to_gtv());
879: operation recall_unclaimed_transfer(
888:     val init_transfer_details = get_init_transfer_details(init_transfer_tx, init_tx_op_index);
890:     val last_hop_blockchain_rid = init_transfer_details.args.hops[init_transfer_details.args.hops.size()-1];
896:     val apply_tx = require(
908:     val asset = assets.Asset(init_transfer_details.args.asset_id);
974: function validate_apply_transfer(
981:     val init_transfer_details = get_init_transfer_details(init_transfer_tx, init_tx_op_index);
1017:         val previous_hop_details = get_apply_transfer_details(previous_hop_tx, op_index);
1036:     val current_hop_blockchain_rid = init_transfer_details.args.hops[hop_index];
1042:     val from_blockchain_rid = previous_hop_tx.body.blockchain_rid;
1045:         val previous_hop_blockchain_rid = init_transfer_details.args.hops[hop_index - 1];
1079: function get_init_transfer_details(tx: gtx_transaction, op_index: integer) {
1080:     val op = tx.body.operations[op_index];
1087:     val tx_rid = tx.body.hash();
1088:     val args = struct<init_transfer>.from_gtv(op.args.to_gtv());
1090:     val auth_op = tx.body.operations[op_index - 1];
1091:     val sender_account_id = auth.extract_account_id(auth_op);
1119: function get_apply_transfer_details(tx: gtx_transaction, apply_op_index: integer) {
1120:     val op = tx.body.operations[apply_op_index];
1127:     val tx_rid = tx.body.hash();
1128:     val args = struct<apply_transfer>.from_gtv(op.args.to_gtv());
1130:     val init_details = get_init_transfer_details(args.init_transfer_tx, args.init_tx_op_index);

### external/crosschain/queries.rell
File: rell/src/lib/ft4/external/crosschain/queries.rell

10: query get_asset_origin_by_id(asset_id: byte_array) {
28: query get_pending_transfers_for_account(
50: query get_last_pending_transfer_for_account(
72: query is_transfer_applied(init_tx_rid: byte_array, init_op_index: integer) {
73:     val transfer = crosschain.applied_transfers @? { .init_tx_rid == init_tx_rid, .init_op_index == init_op_index };
86: query get_apply_transfer_tx(init_tx_rid: byte_array, init_op_index: integer) =
99: query get_asset_origin_filtered(asset_origin_filter: crosschain.asset_origin_filter?, page_size: integer?, page_cursor: text?) {
114: query get_applied_transfers_filtered(applied_transfers_filter: crosschain.transfers_filter?, page_size: integer?, page_cursor: text?) {
128: query get_canceled_transfers_filtered(canceled_transfers_filter: crosschain.transfers_filter?, page_size: integer?, page_cursor: text?) {
142: query get_unapplied_transfers_filtered(unapplied_transfers_filter: crosschain.transfers_filter?, page_size: integer?, page_cursor: text?) {
156: query get_recalled_transfers_filtered(recalled_transfers_filter: crosschain.transfers_filter?, page_size: integer?, page_cursor: text?) {
170: query get_pending_transfers_filtered(pending_transfer_filter: crosschain.pending_transfer_filter?, page_size: integer?, page_cursor: text?) {
184: query get_reverted_transfers_filtered(reverted_transfer_filter: crosschain.transfers_filter?, page_size: integer?, page_cursor: text?) {

---
## 8. Utils and prioritization

### utils/utils.rell
File: rell/src/lib/ft4/utils/utils.rell

2: val MILLISECONDS_PER_DAY = 24 * 60 * 60 * 1000;
4: val RECIPIENT_ID_MAX_SIZE = 1024;
11: struct validation_result {
19: val VALID = validation_result(true, null);
25: function invalid(error: text) = validation_result(false, error);
38: function make_auth_message(message: text) {
49: function derive_nonce(op: gtx_operation, nonce: integer) {
62: function latest_time() =
75: function get_block_height() =
95: function validate_blockchain_rid(blockchain_rid: byte_array, descriptor: text = "blockchain_rid") {
115: function validate_recipient_id(recipient_id: byte_array) {
137: function convert_gtv_to_text(
162:         val dict = map<text, gtv>.from_gtv(gtv);
185:         val l = list<gtv>.from_gtv(gtv);
232: function is_byte_array(gtv) = gtv.to_bytes()[0] == 161;
239: function is_text(gtv) = gtv.to_bytes()[0] == 162;
246: function is_integer(gtv) = gtv.to_bytes()[0] == 163;
253: function is_dict(gtv) = gtv.to_bytes()[0] == 164;
260: function is_list(gtv) = gtv.to_bytes()[0] == 165;
267: function is_big_integer(gtv) = gtv.to_bytes()[0] == 166;
273: function validate_composite_indexes_tx_rids_and_op_index(transaction_rids: list<byte_array>?, op_index: integer?) {
274:     val basic_error_message = "INVALID FILTER: Composite index (transaction_rids, op_index) - ";
294: function validate_composite_indexes_init_tx_rids_and_init_op_index(
297:     val basic_error_message = "INVALID FILTER: Composite index (init_tx_rids, init_op_index) - ";

### core/prioritization/module.rell
File: rell/src/lib/ft4/core/prioritization/module.rell

9: struct priority_state_v1 {
53: query priority_check_v1(tx_body: gtx_transaction_body, tx_size: integer, tx_enter_timestamp: timestamp, current_timestamp: timestamp): priority_state_v1 =
78: function no_op_priority_state() =
92: function no_account_priority_state(priority: decimal) =

---
## 9. JS client declarations

package.json name/version:
@chromia/ft4 1.1.0

### client/lib/ft4/index.ts
import { logger } from "postchain-client";

export * from "./accounts";
export * from "./authentication";
export * from "./registration";
export * from "./admin";
export * from "./asset";
export * from "./ft-session";
export * from "./utils";
export * from "./events";
export * from "./crosschain";
export * from "./transaction-builder";

export const ft = Object.freeze({
  setLogLevel: logger.setLogLevel,
});

ft.setLogLevel(logger.LogLevel.Disabled);

### client/lib/ft4/accounts/operations.ts
import { BufferId, formatter, Operation } from "postchain-client";
import { Amount } from "@ft4/asset";
import { op } from "@ft4/utils";
import { gtv } from "./auth-descriptor";
import { AnyAuthDescriptorRegistration } from "@ft4/accounts";

/**
 * Creates an operation object that can be used to call the `ft4.burn`-operation
 * @param assetId - id of the asset to burn
 * @param amount - how much of the asset to burn
 */
export function burn(assetId: BufferId, amount: Amount): Operation {
  return op("ft4.burn", formatter.ensureBuffer(assetId), amount.value);
}

/**
 * Creates an operation object that can be used to call the `ft4.transfer`-operation
 * @param receiverId - id of the account that will receive this transfer
 * @param assetId - the id of the asset to transfer
 * @param amount - how much of said asset to transfer
 */
export function transfer(
  receiverId: BufferId,
  assetId: BufferId,
  amount: Amount,
): Operation {
  return op(
    "ft4.transfer",
    formatter.ensureBuffer(receiverId),
    formatter.ensureBuffer(assetId),
    amount.value,
  );
}

/**
 * Creates an operation object that can be used to call the `ft4.update_main_auth_descriptor`-operation
 * @param authDescriptor - information for the new main auth descriptor
 */
export function updateMainAuthDescriptor(
  authDescriptor: AnyAuthDescriptorRegistration,
): Operation {
  return op(
    "ft4.update_main_auth_descriptor",
    gtv.authDescriptorRegistrationToGtv(authDescriptor),
  );
}

/**
 * Creates an operation object that can be used to call the `ft4.recall_unclaimed_transfer`-operation
 * @param txRid - the rid of the tx in which the transfer to recall was submitted
 * @param opIndex - the index of the transfer to recall inside the tx pointed out by `txRid`
 */
export function recallUnclaimedTransfer(
  txRid: BufferId,
  opIndex: number,
): Operation {
  return op(
    "ft4.recall_unclaimed_transfer",
    formatter.ensureBuffer(txRid),
    opIndex,
  );
}

/**
 * Creates an operation object that can be used to call the `ft4.add_auth_descriptor`-operation
 * @param authDescriptor - the auth descriptor information to add
 */
export function addAuthDescriptor(
  authDescriptor: AnyAuthDescriptorRegistration,
): Operation {
  return op(
    "ft4.add_auth_descriptor",
    gtv.authDescriptorRegistrationToGtv(authDescriptor),
  );
}

/**
 * Creates an operation object that can be used to call the `ft4.delete_auth_descriptor`-operation
 * @param authDescriptorId - the id of the auth descriptor to delete
 */
export function deleteAuthDescriptor(authDescriptorId: BufferId): Operation {
  return op(
    "ft4.delete_auth_descriptor",
    formatter.ensureBuffer(authDescriptorId),
  );
}

/**
 * Creates an operation object that can be used to call the `ft4.delete_auth_descriptors_for_signer`-operation
 * @param signer - the signer for which to delete auth descriptors
 */
export function deleteAuthDescriptorsForSigner(signer: BufferId): Operation {
  return op(
    "ft4.delete_auth_descriptors_for_signer",
    formatter.ensureBuffer(signer),
  );
}

/**
 * Creates an operation object that can be used to call the `ft4.delete_all_auth_descriptors_except_main`-operation
 */
export function deleteAllAuthDescriptorsExceptMain(): Operation {
  return op("ft4.delete_all_auth_descriptors_except_main");
}

### client/lib/ft4/admin/admin-operations.ts
import { BufferId, Operation, formatter } from "postchain-client";
import { gtv, AnyAuthDescriptorRegistration } from "@ft4/accounts";
import { Amount, CrosschainAssetRegistration } from "@ft4/asset";
import { op } from "@ft4/utils";

/**
 * Creates an operation object for the `ft4.admin.register_account`-operation
 * @param authDescriptor - the auth descriptor data that will be the main auth descriptor of the account
 */
export function registerAccount(
  authDescriptor: AnyAuthDescriptorRegistration,
): Operation {
  return op(
    "ft4.admin.register_account",
    gtv.authDescriptorRegistrationToGtv(authDescriptor),
  );
}

export function addRateLimitPoints(
  accountId: BufferId,
  amount: number,
): Operation {
  return op(
    "ft4.admin.add_rate_limit_points",
    formatter.ensureBuffer(accountId),
    amount,
  );
}

export function registerAsset(
  name: string,
  symbol: string,
  decimals: number,
  iconUrl: string,
): Operation {
  return op("ft4.admin.register_asset", name, symbol, decimals, iconUrl);
}

export function mint(
  accountId: BufferId,
  assetId: BufferId,
  amount: Amount,
): Operation {
  return op(
    "ft4.admin.mint",
    formatter.ensureBuffer(accountId),
    formatter.ensureBuffer(assetId),
    amount.value,
  );
}

export function registerCrosschainAsset(
  asset: CrosschainAssetRegistration,
  originBlockchainRid: BufferId,
): Operation {
  return op(
    "ft4.admin.register_crosschain_asset",
    asset.id,
    asset.name,
    asset.symbol,
    asset.decimals,
    asset.blockchainRid,
    asset.iconUrl,
    asset.type,
    asset.uniquenessResolver,
    formatter.ensureBuffer(originBlockchainRid),
  );
}

### client/lib/ft4/authentication/index.ts
export {
  LoginConfigComplexRule,
  LoginConfigRules,
  LoginConfigRuleVariable,
  LoginConfigRelativeRuleVariable,
  LoginConfigSimpleRule,
  LoginKeyStore,
  LoginOptions,
  LoginConfigOptions,
  SessionWithLogout,
  LoginConfig,
  mapLoginConfigRulesToAuthDescriptorRules,
  deleteDisposableAuthDescriptors,
  createLocalStorageLoginKeyStore,
  createSessionStorageLoginKeyStore,
  createInMemoryLoginKeyStore,
  relativeBlockHeight,
  relativeBlockTime,
  minutes,
  hours,
  days,
  weeks,
  ttlLoginRule,
  getLoginConfig,
  login,
  getConfigFromOptions,
} from "./login";

export {
  Signer,
  KeyStore,
  KeyHandler,
  AuthDataService,
  Authenticator,
  KeyHandlerError,
  AuthHandler,
  SigningError,
} from "./types";

export {
  Signature,
  EvmKeyStore,
  RawSignature,
  EvmSigner,
  Eip1193Provider,
  createGenericEvmKeyStore,
  createInMemoryEvmKeyStore,
  createWeb3ProviderEvmKeyStore,
  createEvmKeyHandler,
  signMessage,
  sliceSignature,
  toRawSignature,
  isEvmKeyStore,
  isEvmSigner,
  evmSigner,
  evmAuth,
  BLOCKCHAIN_RID_PLACEHOLDER,
  ACCOUNT_ID_PLACEHOLDER,
  AUTH_DESCRIPTOR_ID_PLACEHOLDER,
  NONCE_PLACEHOLDER,
  EVM_AUTH,
} from "./evm";

export {
  FtKeyStore,
  FtSigner,
  ftAuth,
  createInMemoryFtKeyStore,
  createFtKeyHandler,
  ftSigner,
  isFtKeyStore,
  isFtSigner,
  FT_AUTH,
} from "./ft";

export { createNoopAuthenticator, noopAuthenticator } from "./noop";

export {
  createAuthenticator,
  hasAuthDescriptorFlags,
  getKeyHandlersForKeyStores,
  isAuthOperation,
} from "./main";

export {
  authDescriptorCounter,
  authMessageTemplate,
  authFlags,
} from "./queries";

### client/lib/ft4/accounts/auth-descriptor/types.ts
import { Buffer } from "buffer";
import { AuthDescriptorRules, RawRules } from "./rules";

export const AuthFlag = Object.freeze({
  Account: "A", // Change Account settings
  Transfer: "T", // Transfer balance
});

export enum AuthType {
  SingleSig = "S",
  MultiSig = "M",
}

export type AuthDescriptor<T extends SingleSig | MultiSig> = {
  id: Buffer;
  accountId: Buffer;
  accountType: string;
  authType: AuthType;
  rules: AuthDescriptorRules | null;
  created: Date;
  args: T;
};

export type AuthDescriptorRegistration<T extends SingleSig | MultiSig> = {
  authType: AuthType;
  args: T;
  rules: AuthDescriptorRules | null;
};

export type AnyAuthDescriptor =
  | AuthDescriptor<SingleSig>
  | AuthDescriptor<MultiSig>;

export type AnyAuthDescriptorRegistration =
  | AuthDescriptorRegistration<SingleSig>
  | AuthDescriptorRegistration<MultiSig>;

export type SingleSig = {
  flags: string[];
  signer: Buffer;
};

export type MultiSig = {
  flags: string[];
  signaturesRequired: number;
  signers: Buffer[];
};

// ======== Server side =======================
export type RawMultiSig = readonly [
  flags: string[],
  signaturesRequired: number,
  signers: Buffer[],
];

export type RawSingleSig = readonly [flags: string[], signer: Buffer];

// ======== Server side request model =========

export type RawAuthDescriptorArgs = RawSingleSig | RawMultiSig;
export type RawAuthDescriptorRegistration<T extends RawAuthDescriptorArgs> =
  readonly [auth_type: number, args: T, rules: RawRules | null];

export type RawAnyAuthDescriptorRegistration =
  | RawAuthDescriptorRegistration<RawSingleSig>
  | RawAuthDescriptorRegistration<RawMultiSig>;

// ======== Server side response model ========

export type RawAnyAuthDescriptor =
  | RawAuthDescriptor<RawSingleSig>
  | RawAuthDescriptor<RawMultiSig>;

export type RawAuthDescriptor<T extends RawAuthDescriptorArgs> = {
  args: T;
  account_id: Buffer;
  account_type: string;
  auth_type: string;
  created: number;
  id: Buffer;
  rules: RawRules | null;
  ctr: number;
};

### client/lib/ft4/authentication/login/types.ts
import { Session } from "@ft4/ft-session";
import { LoginConfigRules } from "./rules";
import { RawRules } from "@ft4/accounts";
import { LoginKeyStore } from "./stores";
import { BufferId } from "postchain-client";

export type LoginConfig = {
  flags: string[];
  rules: LoginConfigRules | null;
};

export type RawLoginConfig = {
  flags: string[];
  rules: RawRules | null;
};

export type LoginOptions = {
  accountId: BufferId;
} & LoginConfigOptions;

export type LoginConfigOptions = {
  loginKeyStore?: LoginKeyStore;
} & (
  | {
      configName: string;
      config?: never;
    }
  | {
      configName?: never;
      config: LoginConfig;
    }
  | {
      configName?: never;
      config?: never;
    }
);

export type SessionWithLogout = {
  session: Session;
  /**
   * Deletes the disposable auth descriptor from the account and the key from memory, making
   * the account inaccessible to this key and the key inaccessible to this machine unless
   * stored elsewhere.
   */
  logout: () => Promise<void>;
};

export class LoginConfigError extends Error {
  constructor(msg?) {
    super(msg);
    this.message = msg;
    this.name = "LoginConfigError";
  }
}

### client/lib/ft4/registration/operations.ts
import { Operation } from "postchain-client";

/**
 * Produces a register_account operation.
 */
export function registerAccount(): Operation {
  return {
    name: "ft4.register_account",
    args: [],
  };
}

### client/lib/ft4/crosschain/operations.ts
import {
  BufferId,
  GTX,
  Operation,
  RawGtx,
  formatter,
  gtx,
} from "postchain-client";
import { Amount } from "@ft4/asset";
import { op } from "@ft4/utils";
import { GtvInitTransferArgs } from "./types";

/**
 * Builds an operation object that can be used to call `ft4.crosschain.init_transfer` operation
 * @param recipientId - id of the account that will receive this transfer
 * @param assetId - the id of the asset that will be transferred
 * @param amount - how much of the specified asset that will be transferred
 * @param hops - a list containing rids of the blockchains that are on the path from the source to the target chain, including the target chain
 * @param deadline - after how many days this transfer can be reverted if not claimed
 */
export function initTransfer(
  recipientId: BufferId,
  assetId: BufferId,
  amount: Amount,
  hops: BufferId[],
  deadline: number,
): Operation {
  return op(
    "ft4.crosschain.init_transfer",
    ...getInitTransferArgs(recipientId, assetId, amount, hops, deadline),
  );
}

function getInitTransferArgs(
  receiverId: BufferId,
  assetId: BufferId,
  amount: Amount,
  hops: BufferId[],
  deadline: number,
): GtvInitTransferArgs {
  return [
    formatter.ensureBuffer(receiverId),
    formatter.ensureBuffer(assetId),
    amount.value,
    hops.map(formatter.ensureBuffer),
    deadline,
  ];
}

/**
 * Builds an operation object that can be used to call `ft4.crosschain.complete_transfer` operation
 * @param initTransferTx - the transaction that contains the init_transfer operation for the transfer to apply
 * @param initTransferOpIndex - the index of the transfer operation inside the `initTransferTx` transaction
 * @param tx - the transaction that was submitted to the previous chain hop on the path which contains the transfer to apply
 * @param operationIndex - the index inside the `tx` object at which the transfer to apply can be found
 * @param targetChainIndex - the index of the current chain on the path
 */
export function applyTransfer(
  initTransferTx: GTX,
  initTransferOpIndex: number,
  tx: GTX,
  operationIndex: number,
  targetChainIndex: number,
): Operation {
  return op(
    "ft4.crosschain.apply_transfer",
    gtx.gtxToRawGtx(initTransferTx),
    initTransferOpIndex,
    gtx.gtxToRawGtx(tx),
    operationIndex,
    targetChainIndex,
  );
}

/**
 * Builds an operation object that can be used to call `ft4.crosschain.complete_transfer` operation
 * @param tx - the transaction that was submitted to the previous chain hop on the path which contains the transfer to complete
 * @param opIndex - the index inside the `tx` object at which the transfer to complete can be found
 */
export function completeTransfer(tx: RawGtx, opIndex: number): Operation {
  return op("ft4.crosschain.complete_transfer", tx, opIndex);
}

/**
 * Builds an operation object that can be used to call `ft4.crosschain.cancel_transfer` operation
 * @param initTransferTx - the transaction that contains the init_transfer operation for the transfer to cancel
 * @param initTransferOpIndex - the index of the transfer operation inside the `initTransferTx` transaction
 * @param tx - the transaction that was submitted to this chain and which contains the transfer to cancel
 * @param operationIndex - the index inside the `tx` object at which the transfer to cancel can be found
 * @param targetChainIndex - the index of the current chain on the path
 */
export function cancelTransfer(
  initTransferTx: GTX,
  initTransferOpIndex: number,
  tx: GTX,
  operationIndex: number,
  targetChainIndex: number,
): Operation {
  return op(
    "ft4.crosschain.cancel_transfer",
    gtx.gtxToRawGtx(initTransferTx),
    initTransferOpIndex,
    gtx.gtxToRawGtx(tx),
    operationIndex,
    targetChainIndex,
  );
}

/**
 * Builds an operation object that can be used to call `ft4.crosschain.unapply_transfer` operation
 * @param initTransferTx - the transaction that contains the init_transfer operation for the transfer to un-apply
 * @param initTransferOpIndex - the index of the transfer operation inside the `initTransferTx` transaction
 * @param tx - the transaction that was submitted to this chain and which contains the transfer to un-apply
 * @param operationIndex - the index inside the `tx` object at which the transfer to un-apply can be found
 * @param targetChainIndex - the index of the current chain on the path
 */
export function unapplyTransfer(
  initTransferTx: GTX,
  initTransferOpIndex: number,
  tx: GTX,
  operationIndex: number,
  targetChainIndex: number,
): Operation {
  return op(
    "ft4.crosschain.unapply_transfer",
    gtx.gtxToRawGtx(initTransferTx),
    initTransferOpIndex,
    gtx.gtxToRawGtx(tx),
    operationIndex,
    targetChainIndex,
  );
}

/**
 * Builds an operation object that can be used to call `ft4.crosschain.revert_transfer` operation
 * @param initTransferTx - the transaction that contains the init_transfer operation for the transfer to revert
 * @param initTransferOpIndex - the index of the transfer operation inside the `initTransferTx` transaction
 * @param tx - the transaction that was submitted to this chain and which contains the transfer to revert
 * @param operationIndex - the index inside the `tx` object at which the transfer to revert can be found
 */
export function revertTransfer(
  initTransferTx: GTX,
  initTransferOpIndex: number,
  tx: GTX,
  operationIndex: number,
): Operation {
  return op(
    "ft4.crosschain.revert_transfer",
    gtx.gtxToRawGtx(initTransferTx),
    initTransferOpIndex,
    gtx.gtxToRawGtx(tx),
    operationIndex,
  );
}

/**
 * Builds an operation object that can be used to call `ft4.crosschain.recall_unclaimed_transfer` operation
 * @param initTransferTx - the transaction that contains the init_transfer operation for the transfer to recall
 * @param initTransferOpIndex - the index of the transfer operation inside the `initTransferTx` transaction
 */
export function recallUnclaimedTransfer(
  initTransferTx: GTX,
  initTransferOpIndex: number,
): Operation {
  return op(
    "ft4.crosschain.recall_unclaimed_transfer",
    gtx.gtxToRawGtx(initTransferTx),
    initTransferOpIndex,
  );
}

---
## 10. Auth flag enforcement (source behavior)

authenticate() calls authenticate_and_return_context(). Previous op must be ft4.ft_auth or ft4.evm_auth (is_auth_op). evm_signatures is not an auth op.
Then: fetch account+descriptor; resolver must accept this descriptor; before_authenticate; rate_limit (spend 1 point); reject EXPIRED AUTH DESCRIPTOR; flags = handler.flags; FT or EVM signature check via check_auth_args; increment ctr; delete_expired except the used descriptor; after_authenticate.
require_mandatory_flags is called only from create_account_with_auth and update_main_auth_descriptor. add_auth_descriptor does not call it. Login descriptors with empty flags are allowed.
auth_flags.mandatory default is [A, T]. Comment says all descriptors must have them; code does not enforce that on add_auth_descriptor.
Handler flags on shipped ops: transfer/burn/recall_unclaimed_transfer/init_transfer/renew_subscription require T. add_auth_descriptor and update_main and delete_all_except_main require A. delete_auth_descriptor and delete_auth_descriptors_for_signer use empty flags plus resolver (self or A). apply/complete/cancel/unapply/revert/recall_unclaimed (crosschain) do not call authenticate.
Built-in evm_signatures whitelist: ft4.register_account, ft4.add_auth_descriptor, ft4.update_main_auth_descriptor.
Built-in auth-op blacklist: nop, timeb, iccf_proof, ft4.ft_auth, ft4.evm_auth, ft4.evm_signatures.
DEFAULT_LOGIN_CONFIG_NAME is default. get_login_config missing default returns flags [] and ttl(1 day). MILLISECONDS_PER_DAY is 86400000.
get_account_id_from_signers: one signer hashes that signer; many signers hash the sorted list.

## 11. Admin must never ship

Import lib.ft4.admin mounts: ft4.admin.register_account, register_asset, register_asset_with_type, mint, add_rate_limit_points.
Import lib.ft4.admin.crosschain mounts: ft4.admin.register_crosschain_asset(id, name, symbol, decimals, issuing_blockchain_rid, icon_url, type, uniqueness_resolver, origin_blockchain_rid).
All of those only check require_admin() = op_context.is_signer(admin_pubkey). Single key. No rotation. No multisig.
module_args.admin_pubkey type is pubkey, not byte_array.
ras_open and ras_transfer_open source comments say they are not desirable for production (spam account creation).

## 12. Transfer and crosschain hops

ft4.transfer(recipient_id, asset_id, amount) requires T. Existing recipient: Unsafe.transfer. Missing: INVALID RECIPIENT unless is_create_on_internal_transfer_enabled (default false).
init_transfer params in source: recipient_id, asset_id, amount, hops, deadline. Requires T. MAX_PATH_LENGTH is 100. Moves funds to ensure_blockchain_account(hops[0]).
apply_transfer(init_transfer_tx, init_tx_op_index, previous_hop_tx, op_index, hop_index): no flags; needs iccf_proof. Last hop credits recipient or create_on_crosschain_transfer.
complete_transfer(final_apply_transfer_tx, op_index) on source.
Failure: cancel_transfer (same 5 args as apply), unapply_transfer(init_tx, init_op, last_tx, last_op, hop_index), revert_transfer(init_tx, init_op, last_tx, last_op), recall_unclaimed_transfer(init_tx, init_op) on target.
No eif_* ops in this library.
register_asset id is (name, blockchain_rid).hash().
asset.symbol is not a lone key. Composite keys use uniqueness_resolver. icon_url is mutable.
Burn issuing-chain error string is: Assets can only be burned on issuing chain. No UNAUTHORIZED BURNING prefix. Mint uses UNAUTHORIZED MINTING.

## 13. Discrepancies vs study-ft4.md

1. Tags after 1.1.0r exist (v1.1.1r, v1.2.0r, v2.x). Docs changelog stops at 1.1.0r and still pin v1.1.0r. 2.x is the TS client; Rell at v2.1.1 is still 1.2.0.
2. max_auth_descriptor_rules sibling key does not exist. Only auth_descriptor.max_rules.
3. auth_flags exists in source with default mandatory A,T. configuration-values page omits it. get_config query returns rate_limit and auth_descriptor only.
4. require_mandatory_flags is main-only. Docs struct comment says all descriptors.
5. delete_auth_descriptor handler flags are empty plus resolver (self or A). Docs short form said fail without A.
6. init_transfer arg order is recipient_id, asset_id, amount, hops, deadline. Docs listed asset, amount, destination, hops, deadline.
7. admin.register_crosschain_asset is 9 args including id, type, uniqueness_resolver. Docs CLI example is the pre-0.8.0r shape.
8. admin_pubkey type is pubkey, not byte_array.
9. asset.symbol is not a key. uniqueness_resolver exists. icon_url is mutable.
10. Burn string is not UNAUTHORIZED BURNING.
11. Rate-limit extendable name is account_rate_limit_config. Wrapper is get_rate_limit_config_for_account.
12. ras_open, ras_transfer_open, ras_transfer_fee are mounted. Docs crawl only printed ras_transfer_subscription.
13. DEFAULT_LOGIN_CONFIG_NAME is the string default.
14. CURRENT_CHAIN_REF is dollar. ANY_REF is star. CURRENT_ACCOUNT_REF is X.
15. Multi-signer account id is hash of sorted signers.
16. JS LoginOptions is config or configName, not top-level flags.
17. get_config also returns auth_descriptor, not only rate_limit.
18. fee_asset: if id is present, name and issuing rid are ignored. Docs said id cannot be paired.
19. No mounted delete_main_auth_descriptor op. Function exists for internal update_main.
20. JS applyTransfer JSDoc incorrectly says complete_transfer. op name is apply_transfer.

Constants printed from source: ACCOUNT_TYPE_USER=FT4_USER, ACCOUNT_TYPE_LOCK=FT4_LOCK, ACCOUNT_TYPE_BLOCKCHAIN=FT4_BLOCKCHAIN, ACCOUNT_TYPE_POOL=FT4_POOL, ACCOUNT_TYPE_FEE=FT4_FEE, ASSET_TYPE_FT4=ft4, EVM_ADDRESS_SIZE=20, FT_PUBKEY_SIZE=33, AUTH_DESCRIPTORS_PER_ACCOUNT_UPPER_BOUND=min(200, config).

Local raw copies: /workspace/chromia-knowledge/raw-ft4-src/v1.1.0r/


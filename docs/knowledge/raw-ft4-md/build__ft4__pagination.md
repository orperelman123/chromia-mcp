On this page
# Pagination

Each query in FT4 has pagination enabled by default. The query will return a paged result containing the result and the cursor to fetch the next page.

The pagination system revolves around two key components:

- `page_cursor`: Points to the position in the data where the next page should begin. 
- `paged_result`: Contains the data for the current page and a cursor for fetching the next page. 
You can use this utility in queries where paginated results are needed, enabling the front-end to request data page by page.

### Key functions and structures​

- `page_cursor`: A struct that keeps track of the position of the last item retrieved. 
- `pagination_result`: Represents a single element of a paginated result containing data of the fetched result and the rowid. 
- `paged_result`: Represents the full response of a paginated query. Contains a list of data and a cursor to fetch the next page. 
- `encode_cursor` & `decode_cursor`: Functions that encode and decode a `page_cursor` into a Base64 string for easier management. 
- `fetch_data_size`: Determines the page size based on the request and module configuration. 
- `make_page`: Creates a paginated response from a list of data, and ensures the page size is respected. 
- `before_rowid`: Retrieves the `rowid` of the last retrieved element from the cursor, used to determine where the next page should start. 
- Key functions and structures
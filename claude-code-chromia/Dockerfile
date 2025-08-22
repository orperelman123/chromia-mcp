FROM postgres:16.9@sha256:ddfe3e8713e3ee5b8f286082cb12512488dfbf3f5a1ecb0b74a42e6055af0a5f AS postgres

FROM registry.gitlab.com/chromaway/core-tools/chromia-cli/chr:latest AS chr

FROM registry.gitlab.com/chromaway/core-tools/management-console/pmc:latest AS pmc

FROM registry.gitlab.com/chromaway/core-tools/chromia-images/claude-code:0.1.0@sha256:8913a8c52b5cafaf48a273ed92c70d9b1f561ca3c2d29ab786b6193e0df535dd

COPY --from=postgres /usr/local/bin/docker-entrypoint.sh /usr/local/bin/postgres-entrypoint.sh
COPY claude-code-chromia/docker/docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh
ENV PGDATA=/pgdata
ENV POSTGRES_INITDB_ARGS="--lc-collate=C.UTF-8 --lc-ctype=C.UTF-8 --encoding=UTF-8"
ENV POSTGRES_DB=postchain
ENV POSTGRES_USER=postchain
ENV POSTGRES_PASSWORD=postchain
RUN mkdir /docker-entrypoint-initdb.d

COPY --from=chr /usr/share/chr /usr/share/chr
COPY --from=chr /usr/bin/chr /usr/bin/

COPY --from=pmc /opt/chromaway/postchain /opt/chromaway/postchain
COPY --from=pmc /usr/bin/pmc /usr/bin/

COPY app/build/libs/chromia-mcp-server.jar /opt/chromia/

ENTRYPOINT ["docker-entrypoint.sh"]

FROM alpine:latest

RUN apk add --no-cache xz tar

# Zig 0.15.2 for Linux aarch64 (runs on Apple Silicon via Colima/Docker)
COPY zig-aarch64-linux-0.15.2.tar.xz /tmp/
RUN mkdir -p /opt/zig \
    && tar -xf /tmp/zig-linux-0.15.2.tar.xz -C /opt/zig --strip-components=1 \
    && rm /tmp/zig-linux-0.15.2.tar.xz

ENV PATH="/opt/zig:${PATH}"
WORKDIR /src

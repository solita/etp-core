# Docker

Build docker image `etp-db`:

```
rm -rf target
clj -A:uberjar
docker build . --tag etp-db
```

Run migrations from image `etp-db`:

```
docker run -it --rm etp-db ./db.sh migrate
```


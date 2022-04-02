#todo temp solution
set -e
docker build -t artcoded/drive-sync .
docker tag artcoded/drive-sync artcoded/drive-sync
docker push artcoded/drive-sync

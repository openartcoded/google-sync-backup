#todo temp solution
set -e
docker build -t nbittich/drive-sync .
docker tag nbittich/drive-sync nbittich/drive-sync
docker push nbittich/drive-sync

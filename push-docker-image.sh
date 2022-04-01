#todo temp solution
set -e
docker build -t artcoded/drive-sync .
docker tag artcoded/drive-sync artcoded:5000/artcoded/drive-sync
docker push artcoded:5000/artcoded/drive-sync

set -e # fail script on error

if [[ -z "$1" || -z "$2" ]]
then
  echo "version mut be provided"
  exit -1;
fi

releaseVersion=$1
nextVersion=$2-SNAPSHOT

echo "release" $releaseVersion ", next" $nextVersion

mvn --batch-mode -Dtag=$releaseVersion release:prepare \
                 -DreleaseVersion=$releaseVersion \
                 -DdevelopmentVersion=$nextVersion

mvn release:clean
git pull

git checkout $releaseVersion
docker build -t nbittich/drive-sync:v$releaseVersion .
docker tag nbittich/drive-sync:v$releaseVersion nbittich/drive-sync:v$releaseVersion
docker push nbittich/drive-sync:v$releaseVersion

git checkout main

# Google drive sync

sync a folder to google drive

## How to

```
  google-drive-sync:
    image: artcoded/drive-sync
    networks:
      artcoded:
    volumes:
      - ./data/backend/dump:/var/artcoded/data
      - ./data/drive-sync:/var/drive-sync/
      - ./config/drive-sync:/usr/config
    environment:
      PATH_TO_SYNC: /var/artcoded/data
      IDEM_POTENT_FILE_PATH: /var/drive-sync/idempot.dat
      DRIVE_CREDENTIALS_FILE: /usr/config/artcoded-cred.json
      DRIVE_SHARED_DIRECTORY: "v2022.artcoded-dump"
      APPLICATION_NAME: "artcoded"
      ARTEMIS_USER: artemis
      ARTEMIS_URL: tcp://artemis:61616
      ARTEMIS_PASSWORD: "CHANGEME"
```

- on google cloud console, create a project
- add drive api access to the project
- create credentials, must be service account
- save credentials as a file
- go to you drive account, create a folder and share it with your service account (write/read accesses)
- add the docker image to your docker compose stack and change the following environment properties:
  - `DRIVE_CREDENTIALS_FILE` => credentials file
  - `DRIVE_SHARED_DIRECTORY` => shared directory name
  - `APPLICATION_NAME` => can be anything

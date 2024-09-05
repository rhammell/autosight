# autosight
A metagraph powered Android app that allows users to earn crypto tokens while driving

# Overview Video

# Project Setup
The following instructions cover the setup process for each AutoSight component, incluing the image upload server, metagraph, Android application, and web application.

## Dependencies
Install the following required dependencies:
- [Python3](https://www.python.org/downloads/)
- [Node JS](https://nodejs.org/en)
- [Euclid Development Enivonment dependencies](https://docs.constellationnetwork.io/sdk/guides/quick-start/#install-dependencies)
- [Android Studio](https://developer.android.com/studio)

## Image Server
The image server is built using the Python Flask microframework. Launching the server establishes an `/upload` endpoint, which accepts images through HTTP requests and saves them locally to the disk.

In a new terminal window, navigate to the `image_upload` directory within the project repository:
```bash
cd autosight/image_upload
```

Install the required Python packages:
```bash
pip3 install -r requirements.txt
```

Launch the server: 
```bash
python3 server.py
```

The server runs on the fault port (5050) and is now available at `http://localhost:5050`.

## Metagraph


## Android App

## Web App

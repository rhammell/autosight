from flask import Flask, jsonify, request, send_from_directory
import json, os
import base64
from datetime import datetime

app = Flask(__name__)

# Ensure the 'images' directories exist
for directory in ['images']:
    if not os.path.exists(directory):
        os.makedirs(directory)

# Define and create image upload folder
IMAGE_UPLOAD_FOLDER = 'images'
if not os.path.exists(IMAGE_UPLOAD_FOLDER):
    os.makedirs(IMAGE_UPLOAD_FOLDER)    

@app.route('/upload', methods=['POST'])
def upload_image():
    try:
        if 'image' not in request.files:
            return jsonify({"error": 'No image part in the request'}), 400
        
        file = request.files['image']
        if file.filename == '':
            return jsonify({"error": 'No selected file'}), 400
        
        if file:
            filename = os.path.join(IMAGE_UPLOAD_FOLDER, file.filename)
            file.save(filename)
            relative_path = os.path.relpath(filename, start=os.curdir)
            return jsonify({"message": 'File uploaded successfully', "path": relative_path}), 200
    
    except Exception as e:
        print(str(e))
        return jsonify({"error": str(e)}), 400

@app.route('/images/<filename>', methods=['GET'])
def serve_image(filename):
    return send_from_directory('images', filename)

if __name__ == "__main__":
    app.run(host='0.0.0.0', port=5050)
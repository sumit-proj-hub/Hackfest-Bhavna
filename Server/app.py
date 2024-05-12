import os
import struct
import subprocess
import time
from io import BytesIO
from pathlib import Path

import torch
import torch.nn as nn
import torch.nn.functional as F
from PIL import Image
from flask import Flask, request, make_response
from torchvision import transforms

app = Flask(__name__)
app.config['UPLOAD_FOLDER'] = './uploads'
app.config['MAX_CONTENT_LENGTH'] = 512 * 1024 * 1024

device = torch.device("cpu")


class CustomClassifier(nn.Module):
    def __init__(self, input_size, num_classes):
        super(CustomClassifier, self).__init__()
        self.fc = nn.Linear(input_size, num_classes)

    def forward(self, x):
        x = self.fc(x)
        x = F.softmax(x, dim=1)
        return x


# Load the saved model
model_path = "model_f1_letsee.pth"
loaded_model = torch.load(model_path, map_location=device)

# Define transformations for preprocessing the input image
transform = transforms.Compose([
    transforms.Resize((224, 224)),  # Resize the image
    transforms.ToTensor(),  # Convert to tensor
    transforms.Normalize(0.5, 0.5)  # Normalize the pixel values
])


def extract_frames(video_path, output_dir, frame_rate=1.0):
    completed_process = subprocess.run(
        ["ffmpeg", "-i", video_path, "-r", str(frame_rate), f"{output_dir}/%d.jpg"],
        capture_output=True)
    if completed_process.returncode != 0:
        raise ValueError(f"{video_path} is not a video file")


def result_from_image(image_path):
    input_image = Image.open(image_path).convert('L').convert('RGB')
    input_tensor = transform(input_image).unsqueeze(0)  # Add batch dimension
    input_tensor = input_tensor.to(device)
    with torch.no_grad():
        result = loaded_model(input_tensor)
    result = (result * 100).tolist()[0]
    print(result)
    output = bytearray(b'\0' * 32)
    offset = 0
    for value in result:
        bits = struct.pack("!f", value)
        output[offset:offset + 4] = list(bits)
        offset += 4
    return output


def result_from_video(video_path):
    temp_dir = f"temp.{int(time.time() * 1000)}"
    os.mkdir(temp_dir)
    try:
        extract_frames(video_path, temp_dir, 4)
    except ValueError:
        os.rmdir(temp_dir)
        raise
    temp_path = Path(temp_dir)
    frames = list(temp_path.iterdir())
    output = bytearray(b'\0' * len(frames) * 32)
    offset = 0
    for frame in frames:
        output[offset:offset + 32] = result_from_image(frame.absolute())
        offset += 32
        frame.unlink()
    temp_path.rmdir()
    return output


@app.route('/upload', methods=['POST'])
def upload():
    if not request.form.__contains__("fileType"):
        return "File Type not found", 400
    file_type = request.form["fileType"]
    if file_type not in ('Image', 'Video'):
        return "Unsupported File Type", 400

    file = request.files['file']
    time_millis = int(time.time() * 1000)
    file_path = os.path.join(app.config['UPLOAD_FOLDER'], f"{time_millis}.{file_type}")
    file.save(file_path)

    try:
        output = result_from_image(file_path) if file_type == "Image" else result_from_video(
            file_path)
        os.unlink(file_path)
        bio = BytesIO(output)
        response = make_response(bio.read())
        response.headers.set('Access-Control-Allow-Origin', '*')
        response.headers.set('Content-Type', 'application/octet-stream')
        return response
    except ValueError as e:
        os.unlink(file_path)
        print(e)
        return "File not supported", 403


if __name__ == "__main__":
    app.run(host="0.0.0.0", debug=True)

import os
import struct
import subprocess
import time
from io import BytesIO
from pathlib import Path

from flask import Flask, request, make_response

app = Flask(__name__)
app.config['UPLOAD_FOLDER'] = './uploads'
app.config['MAX_CONTENT_LENGTH'] = 512 * 1024 * 1024


def extract_frames(video_path, output_dir, frame_rate=1.0):
    completed_process = subprocess.run(
        ["ffmpeg", "-i", video_path, "-r", str(frame_rate), f"{output_dir}/%d.jpg"],
        capture_output=True)
    if completed_process.returncode != 0:
        raise ValueError(f"{video_path} is not a video file")


def result_from_image(image_path):
    result = {
        "anger": 10.0,
        "contempt": 60.0,
        "fear": 20.0,
        "happiness": 40.0,
        "neutral": 50.0,
        "sadness": 10.0,
        "surprise": 10.0,
        "disgust": 1.0
    }

    output = bytearray(b'\0' * 32)
    offset = 0
    for value in result.values():
        bits = struct.pack("!f", value)
        output[offset:offset + 4] = list(bits)
        offset += 4
    return output


def result_from_video(video_path):
    temp_dir = f"temp.{int(time.time() * 1000)}"
    os.mkdir(temp_dir)
    try:
        extract_frames(video_path, temp_dir, 2)
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
        response.headers.set('Content-Type', 'application/octet-stream')
        return response
    except ValueError:
        os.unlink(file_path)
        return "File not supported", 403


if __name__ == "__main__":
    app.run(host="0.0.0.0", debug=True)

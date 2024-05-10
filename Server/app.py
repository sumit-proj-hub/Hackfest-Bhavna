import os
import struct
import subprocess
import time

from flask import Flask, request

app = Flask(__name__)
app.config['UPLOAD_FOLDER'] = './uploads'
app.config['MAX_CONTENT_LENGTH'] = 512 * 1024 * 1024


def extract_frames(video_path, output_dir, frame_rate=1):
    subprocess.run(f'ffmpeg -i ""{video_path}"" -r {frame_rate} ""{output_dir}/%d.jpg"')


def get_result_from_image(image_path):
    result = {
        "anger": 1.0,
        "contempt": 1.0,
        "fear": 1.0,
        "happiness": 1.0,
        "neutral": 1.0,
        "sadness": 1.0,
        "surprise": 1.0,
        "disgust": 1.0
    }

    output = bytearray(b'\0' * 32)
    offset = 0
    for value in result.values():
        bits = struct.pack("!f", value)
        output[offset:offset + 4] = list(bits)
        offset += 4
    return output


@app.route('/upload', methods=['POST'])
def upload():
    file_type = request.form["fileType"]
    if file_type not in ('Image', 'Video'):
        return "Unsupported File Type", 400
    file = request.files['file']
    time_millis = int(time.time() * 1000)
    file.save(os.path.join(app.config['UPLOAD_FOLDER'], f"{time_millis}.{file_type}"))
    time.sleep(20)
    return "File Uploaded"


if __name__ == "__main__":
    app.run(host="0.0.0.0", debug=True)

# Hackfest-Bhavna
## Problem Statement
In an increasingly digital world, where visual communication is prevalent across various platforms, there exists a growing need for automated systems capable of accurately discerning and understanding human emotions depicted in images. However, the complexity and nuances of emotional expression present significant challenges for traditional image processing techniques. Developing a robust machine learning model that can effectively predict emotions from images is paramount for numerous applications across industries such as healthcare, security, entertainment, and customer service.

## Approach
The project is based on a Advanced Computer Vision Techniques with Convolutional Neural Network that has been trained on a dataset that contains various example images of individuals of all types showing various emotions to get the most generalized model that can detect any image without fail. This Machine Learning Model detects an individual facial features based on the pixels of the images, and goes up to detect individual facial features, and thus it is able to detect the emotions of an individuals. For image processing the app will run the image through various scripts to take images and convert them into desirable input for the neural network. The app will provide stability and coordination between all the python scripts. Steps to achieve completion are :-

## Tech Stack
* *Python*: The CNN model will be coded on python. The scripts to parse data will also be coded on python.
* *Hugging Face*: Library to import pretrained models.
* *Albumentations* : Used for image augmentations.
* *Tensorflow and PyTorch*: These libraries will required to build and train machine learning model.
* *Flask* : It will be used to build the server.
* *ffmpeg* : To extract frames from videos and to process the image.
* *Android Studio with Kotlin*: To build the mobile application
* *YCharts*: Library to display histogram in the application.

## Basic Workflow
* *Collecting Data*: We have collected quality data from trusted sources like Kaggle that provide quality images.
* *Making the Model*: We will develop an efficient and stable CNN network.
* *Training the Model*: The model will be trained on the dataset we collected and the model will be tested on a cross validation set so that the neither overfitting or underfitting affects the data.
* *Developing input scripts*: Using various python libraries we will develop input system so that the images or videos entered by the user is easily made to be used by the model.
* *Developing the server*: Server takes input of the media runs it through each of the scripts to get the data and finally respond with a result.
* *Developing the application*: The application will allow the user to capture photos and videos or pick them from their device gallery. Then the application will send the data to the server where it further processes it. It will display the results in the form of a graphical histogram. For videos the histogram will be dynamic according to the frame of the video.

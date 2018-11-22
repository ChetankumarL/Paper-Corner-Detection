# Paper-Corner-Detection
Detection of four corners of a document



This is an android project written in java which detects paper or rectangle by opencv, you can take a picture and it will crop the picture automatically.

Steps:

1- You need to reduce noises with gaussian blur and find all the contours.

2- Find and list all the contours' areas.

3- The largest contour will be nothing but the painting.

4- Now use perpective transformation to transform your shape to a rectangle.

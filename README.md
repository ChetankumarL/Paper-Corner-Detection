# Paper-Corner-Detection
Detection of four corners of a document

this is an android project write in java which detect paper or rectangle by opencv, you can take a picture and it will crop automatically 

Steps:

1- you need to reduce noises with gaussian blur and find all the contours

2- find and list all the contours' areas.

3- the largest contour will be nothing but the painting.

4- now use perpective transformation to transform your shape to a rectangle.

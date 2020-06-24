# Cain
## Convolutional-Filter Code-Generator for Cellular-Processor-Arrays

### Installation
 1. `git clone https://github.com/ed741/cain.git`
 2. `cd cain`
 3. `mvn install`
 
 Installing using Maven will run tests and so should confirm that Cain is working.
 
### Using Cain

Cain uses a JSON input format to define the filter and search parameters to use. Examples can be found in `./examples`.

###### To run an example use:

    java -jar target/cain-2.0.jar examples/sobel.json


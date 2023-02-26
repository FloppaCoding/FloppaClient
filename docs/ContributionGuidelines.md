# Contribution Guidelines

* [Code Standards](#Code-Standards)
    *   [Philosophy](#philosophy)
    *   [Requirements](#requirements)

## Code Standards

### Philosophy
The priority when writing your code should be readability.

### Requirements

#### Comments
All Classes you make should have a documentation comment that gives at least a short summary of what that class is 
for and contain a tag which declares the author. This looks like:
    
    /**
     * A class for storing information about ... and handling ...
     *
     * @author AuthorName
     */
    class MyClass() { //... }

Additionally, documentation comments for at least the public methods are also very welcome.
A good rule of thumb is that if the functionality and usage of something is not trivial from just the name, you 
should add a comment explaining it.

#### Naming conventions

+ Classes and Objects should have a capitalized name with camel case. Example: MyClass.
+ variables and methods should start with a lower case letter and use camel case. Example: myMethod
+ constants and enums should use all caps and underscores for separating words. Example: MY_CONSTANT
+ Do not abbreviate words.

A general rule to follow is to give your variables as descriptive names as possible.



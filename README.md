# Cain
## Convolutional-Filter Code-Generator for Cellular-Processor-Arrays


![](https://github.com/ed741/cain/workflows/Java%20CI%20with%20Maven/badge.svg)

### Installation
 1. `git clone https://github.com/ed741/cain.git`
 2. `cd cain`
 3. `mvn install`
 
 Installing using Maven will run tests and so should confirm that Cain is working.
 
### Using Cain

Cain uses a JSON input format to define the filter and search parameters to use. Examples can be found in `./examples`.

###### To run an example use:

    java -jar target/cain-3.1.jar examples/sobel.json

### JSON Format

| token id          | valid value   | Default | Description  |
| -------------     |:-------------:|:-------:| ------------ |
| id                | Any String    |         | The arbitary name of this search configuration. |
| verbose           | Integer       | 10      | This value controls the verbosity of the setup and checking of the search, not the search itself (see 'runConfig' for search verbosity rules) less than 0 means no output, 0 means critical only, and so on, more than 10 will print debug information. |
| goalSystem        |               |         | This determines the representation of a Goal in Cain. |
| .                 | "Kernel3D"    |         | Use a 3D kernel as the goal (this can represent a 2D kernel with channels) |
| .                 | "BankedKernel3D"|       | Use a banked version of a 3D kernel as the goal (this can represent a 2D kernel with channels) |
| filter            | Filter (see below) |    | The mapping from Registers to the kernels to be compiled. |
| registerAllocator | RegisterAllocator (see below) | | The registerAllocator to use.
| runConfig         | RunConfig (see below) | | The run configutation for Cain, including parameters for search time, traversal algorithm, and cost function. |
| pairGen           | PairGen (see below) |   | The pair generation configuration, including what archatecture to target and any configuration parameters to give the pairGenerator for that archatecture. |

#### Kernel3D Goal Approximation
When using the Kernel3D Goal-System kernels must be approximated using the following parameters:

| token id      | valid value     | Description  |
| ------------- |:---------------:| ------------ |
| 3d            | Boolean         | whether the filter should be considered 3-dimensional or if the 3rd dimension is for input channels. This effectively defines if the inner most array of the kernel should be centered or 0 indexed. |
| maxApproximationDepth | Integer | The maximum number of divisions by 2 that we allow when approximating the the filter for binary encoding. Aka the resolution of the approximation, a value of 5 would mean the filter weights are approximate no closer than to the closest 1/32nd. |
| maxApproximationError | Double  | The allowable cumulative error of the approximation of a filter. A lower error means higher accuracy of results but if the weights are not simple fractions with binary number denominators then program length will increase significantly. |

#### Filter 
The Filter JSON object is a mapping from Register to the kernel to generate in that register.
Each kernel object has an "array" of the weights, and an optional Doubles "depth" and "scale". Every weight in the kernel is multiplied by scale * 2^depth. Default values for scale and depth are 1 and 0 respectively. Weights are interpreted as Doubles; scale and depth are just used to make reading kernels easier.

###### Example:
```
  "filter":{
    "A": {"depth":-6,
      "array":
        [ [ 0, 1, 2, 1, 0],
          [ 1, 4, 6, 4, 1],
          [ 2, 6, 10, 6, 2],
          [ 1, 4, 6, 4, 1],
          [ 0, 1, 2, 1, 0]
        ]
    },
    "B": {"scale":0.0625,
      "array":[ [[1,1], [2,2],[1,1]],
                [[2,2], [4,4],[2,2]],
                [[1,1], [2,2],[1,1]]
      ]
    
    }
  }
  ```
  If 3d is false then this Filter will compute an approximated 5x5 Guassian filter into A from channel 0 aswell as a 3x3 guassian filter on channels 0 and 1 added together into Register B.
  
#### RegisterAllocator
#####registerAllocator.name:"linearScan"
| token id          | valid value   | Default | Description  |
| -------------     |:-------------:|:-------:| ------------ |
| availableRegisters | Array of Strings | | The available Registers to use in computation (registers in the 'filter' mapping are added automatically if not present' Registers must be Strings containing only the characters 'A' to 'Z', for example `"availableRegisters":["A","B","C","D","E","F"],` |
| initialRegisters | Array of Strings |   | In the same format as 'availableRegisters' this defines the input registers. For single channel inputs use a singleton array, for 2d-multichannel filters the order of registers order of channels described in the innermost array of the kernel. |

#### RunConfig

The RunConfig defines the behaviour of the Reverse Search Algorithm, but not any of the Pair Generation or Huristics.

| token id      | valid value   | Description  |
| ------------- |:-------------:| ------------ |
| searchTime    | Integer       | The timeout for searching|
| timeOut       | Boolean       | Whether or not Cain should timeout when searching. If Cain is set to use more than 1 worker it will never stop by itself, even if all nodes have been explored |
| workers       | Integer       | The number for worker threads to use (worker threads search semi-independently using different instances of the same traversal algorithm). This does not effect multi-threading performed within the pairGeneration invocation of a worker thread |
| traversalAlgorithm |  | The traversal Algorithm to use. SOT is recommended. |
| .             | "SOT"         | Child-Generator-Deque-Search - The recommended algorithm |
| .             | "BFS"         | Breadth-First-Search |
| .             | "DFS"         | Depth-First-Search |
| .             | "HOS"         | Heir-Ordered-Search (a priority queue of children are stored such that all 1st children are visited before 2nd children, 2nd before 3rd, and so on
| .             | "BestFirstSearch"| The Heuristic used in the BestFirstSearch is the "costFunction" ||
| costFunction  |  | The cost function to use to compare plans, used to decide if a state has been seen before in a shorter plan, and used to determine the best plan found.|
| .             | "CircuitDepth"| The Maximum circuit depth by 'search steps' regardless of the cost of particular instructions |
| .             | "InstructionCost"| The total cost of every instruction in the plan so far |
| .             | "CircuitDepthThenLength" | First order by the "CircuitDepth" metric then "PlanLength" |
| .             | "LengthThenCircuitDepth" | First order by the "PlanLength" metric then "CircuitDepth" |
| .             | "PlanLength" | Order by the number of 'search steps' in the plan aka the search depth reguardless of the cost of particular instructions |
| liveCounter   | Boolean       | If a live indication of search progress should be printed during the search. |
| livePrintPlans | Integer      | If >0 then finding a new plan is announced as the search finds it. If > 1 then that plan's steps are also printed. |
| quiet         | Boolean       | when set to false Cain will not announce the search starting and stopping or print search statistics. |
| initialMaxDepth | Integer     | The initial maximum number of instruction allowed in a plan before giving up on finding a valid plan. |
| forcedDepthReduction| Integer | When a new valid plan is found the MaxDepth is set to the length of that plan minus this value. | 
| initialMaxCost | Integer      | The initial MaxCost, the  highest cost a plan is allowed. more costly plans are not searched. |
| forcedCostReduction | Integer | When a new valid plan is found the MaxCost is set to the cost of that plan minus this value. |
| allowableAtomsCoefficient | Integer | States with more atoms than the filter we are generating multiplied by this value, plus the atoms in the inital Goals are rejected and not searched |
| goalReductionsPerStep | Integer | The number of goals that can be removed from the State in one transformation. This allows Cain to prune States with more exess goals in the goal-bag than instructions left before MaxDepth. |
| goalReductionsTolerance | Integer | This value is added to the calculation for pruning by "goalReductionsPerStep" to allow for a lower "goalReductionsPerStep" if only a few instructions might reduce the number of goals by more than "goalReductionsPerStep". | 


#### PairGen

This defines what PairGeneration to use and so what architecture to target and what heuristics to use. Pair Generation is dependent on the Goal-System.
example:
```
"pairGen":{
    "name": "Scamp5AnalogueArrayGoal",
    "configGetter": {
        "name":"Threshold",
        "ops":"all",
        "threshold":10
        }
  }
  ```

#####pairGen.name:"Scamp5*"
There are 4 PairGenerators currently available for the Scamp5 architecture.

| Goal System | Implementation |  Digital                 |                  Analogue |
| ----------- | -------------- | ------------------------ | ------------------------- |
| Kernel3D    | Atom Goal      | "Scamp5DigitalAtomGoal"  | "Scamp5AnalogueAtomGoal"  |
| Kernel3D    | Array Goal     | "Scamp5DigitalArrayGoal" | "Scamp5AnalogueArrayGoal" |

Cain can compile code that will run using Scamp5's Analogue functionality - this is the most mature mode; or using the Digital registers.
Atom Goal and Array Goal refer to the underlying data structures that implement the Kernel3D interface. Atom is the original implementation based on AUKE's representation. Array Goals are an alternative format to store the same information more efficiently. We can think of these as sparse and dense formats, where Atom Goals are sparse and Array Goals are dense, but Array Goals are more computationally efficient. The  pair-generation code is agnostic to this choice since it operates on the Kernel3D interface.

######pairGen.name:"Scamp5Analogue*"

When using an analogue Scamp5 PairGen the following json options must be supplied:

| token id      | valid value   | Description  |
| ------------- |:-------------:| ------------ |
| configGetter  | Threshold   | This dynamically switches how to perform the pair-generation based on the current goals|
| .             | Exhaustive   | Enumerate all options that do not make the problem 'larger - and some that do sorted by a heuristic|
| .             | AtomDistance        | Produce far fewer 'probably the best' options without a heuristic |
| .             | AtomDistanceSorted  | Produce far fewer 'probably the best' options sorted by a heuristic |

######Scamp5Analogue*.configGetter.name:"Threshold"
This config Getter requires the following options inside:

| token id      | valid value   | Description  |
| ------------- |:-------------:| ------------ |
| threshold     | Integer   | when the sum of abs(values) > threshold, use a Non-Exhaustive pairGen   |
| heuristic     | "Pattern"   | Use the Pattern Heuristic - that considers the sizes, and patterns between kernels to estimate cost |
| ops           | "all"  | use all available Analogue instructions |
| .             | "basic"  | use basic Analogue instruction as used in AUKE |

######Scamp5Analogue*.configGetter.name:"Exhaustive"
This config Getter requires the following options:

| token id      | valid value   | Description  |
| ------------- |:-------------:| ------------ |
| heuristic     | "Pattern"   | Use the Pattern Heuristic - that considers the sizes, and patterns between kernels to estimate cost |
| ops           | "all"  | use all available Analogue instructions |
| .             | "basic"  | use basic Analogue instruction as used in AUKE |

######Scamp5Analogue*.configGetter.name:"AtomDistance"
This config Getter requires the following options:

| token id      | valid value   | Description  |
| ------------- |:-------------:| ------------ |
| ops           | "all"  | use all available Analogue instructions |
| .             | "basic"  | use basic Analogue instruction as used in AUKE |

######Scamp5Analogue*.configGetter.name:"AtomDistanceSorted"
This config Getter requires the following options:

| token id      | valid value   | Description  |
| ------------- |:-------------:| ------------ |
| heuristic     | "Pattern"   | Use the Pattern Heuristic - that considers the sizes, and patterns between kernels to estimate cost |
| ops           | "all"  | use all available Analogue instructions |
| .             | "basic"  | use basic Analogue instruction as used in AUKE |


######pairGen.name:"Scamp5Digital*"
To produce code got the 1-bit registers in Scamp we must decide how the virtual registers (defined above) translate into physical registers.
When using a digital Scamp5 PairGen the following json options must be supplied:

| token id      | valid value   | Description  |
| ------------- |:-------------:| ------------ |
| configGetter  | Threshold   | This dynamically switches how to perform the pair-generation based on the current goals|
| .             | Exhaustive   | Enumerate all options that do not make the problem 'larger - and some that do sorted by a heuristic|
| .             | AtomDistance        | Produce far fewer 'probably the best' options without a heuristic |
| .             | AtomDistanceSorted  | Produce far fewer 'probably the best' options sorted by a heuristic |
| bits          | Integer          | the number of bits per value stored. Scamp5 doesn't have that many physical registers but we can always compile as if we had more for the science |
| scratchRegs   | Array of Strings    | Registers not used for kernel outputs or values but to facilitate operations |
| regMapping    | Map of String to Array of String | A mapping from at least all the virtual registers defined above to the list of physical 1-bit registers that will hold the values |


######Scamp5Digital*.configGetter.name:"Threshold"
This config Getter requires the following options inside:

| token id      | valid value   | Description  |
| ------------- |:-------------:| ------------ |
| threshold     | Integer   | when the sum of abs(values) > threshold, use a Non-Exhaustive pairGen   |
| heuristic     | "Pattern"   | Use the Pattern Heuristic - that considers the sizes, and patterns between kernels to estimate cost |

######Scamp5Digital*.configGetter.name:"Exhaustive"
This config Getter requires the following options:

| token id      | valid value   | Description  |
| ------------- |:-------------:| ------------ |
| heuristic     | "Pattern"   | Use the Pattern Heuristic - that considers the sizes, and patterns between kernels to estimate cost |

######Scamp5Digital*.configGetter.name:"AtomDistance"
This config Getter requires no extra information so ```configGetter:{name:"AtomDistance"}``` will work.

######Scamp5Digital*.configGetter.name:"AtomDistanceSorted"
This config Getter requires the following options:

| token id      | valid value   | Description  |
| ------------- |:-------------:| ------------ |
| heuristic     | "Pattern"   | Use the Pattern Heuristic - that considers the sizes, and patterns between kernels to estimate cost |




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

    java -jar target/cain-2.0.jar examples/sobel.json

### JSON Format

| token id      | valid value   | Default | Description  |
| ------------- |:-------------:|:-------:| ------------ |
| id            | Any String    |         | The arbitary name of this search configuration. |
| verbose       | Integer       | 10      | This value controls the verbosity of the setup and checking of the search, not the search itself (see 'runConfig' for search verbosity rules) less than 0 means no output, 0 means critical only, and so on, more than 10 will print debug information. |
| goalSystem    | "Atom"        |         | This determines the representation of a Goal in Cain. |
| filter        | Filter (see below) |    | The mapping from Registers to the kernels to be compiled. |
| availableRegisters | Array of Strings | | The available Registers to use in computation (registers in the 'filter' mapping are added automaticly if not present' Registers must be Strings containing only the characters 'A' to 'Z', for example `"availableRegisters":["A","B","C","D","E","F"],` |
| initialRegisters | Array of Strings |   | In the same format as 'availableRegisters' this defines the input registers. For single channel inputs use a singleton array, for 2d-multichannel filters the order of registers order of channels described in the innermost array of the kernel. |
| runConfig     | RunConfig (see below) | | The run configutation for Cain, including parameters for search time, traversal algorithm, and cost function. |
| pairGen       | PairGen (see below) |   | The pair generation configuration, including what archatecture to target and any configuration parameters to give the pairGenerator for that archatecture. |

#### Atom Goal Approximation
When using the Atom Goal-System kernels must be approximated using the following parameters:

| token id      | valid value     | Description  |
| ------------- |:---------------:| ------------ |
| 3d            | Boolean         | whether the filter should be concidered 3-dimentional or if the 3rd dimention is for input channels. This effectivly defines if the inner most array of the kernel should be centered or 0 indexed. |
| maxApproximationDepth | Integer | The maximum number of divisions by 2 that we allow when approximating the the filter for binary encoding. AKa the resolution of the appoimation, a value of 5 would mean the filter weights are approimated no closer than to the closest 1/32nd. |
| maxApproximationError | Double  | The allowable cumaltive error of the approximation of a filter. A lower error means higher acuuracy of results but if the weights are not simple fractions with binary number denominators then program length will increase significantly. |

#### Filter 
The Filter JSON object is a mapping from Reguster to the kernel to generate in that register.
Each kernel object has an "array" of the weights, and an optional Doubles "depth" and "scale". Every weight in the kernel is multiplied by scale * 2^depth. Defualt values for scale and depth are 1 and 0 respectively. Weights are interpreted as Doubles; scale and depth are just used to make reading kernels easier.

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
  
  
#### RunConfig

The RunConfig defines the behaviour of the Reverse Search Algorithm, but not any of the Pair Generation or Huristics.

| token id      | valid value   | Description  |
| ------------- |:-------------:| ------------ |
| searchTime    | Integer       | The timeout for searching|
| timeOut       | Boolean       | Whether or not Cain should timeout when searching. If Cain is set to use more than 1 worker it will never stop by itself, even if all nodes have been explored |
| workers       | Integer       | The number for worker threads to use (worker threads search semi-independantly using different instances of the same traversal algorithm). This does not effect multi-threading performed within the pairGeneration inviocation of a worker thread |
| traversalAlgorithm | "SOT", "BFS", "DFS", "HOS", "BestFirstSearch" | The traversal Algorithm to use. SOT stands for Stow-Optimised-Traversal, and HOS stands for Heir Ordered Seatch. SOT is recommended. The Huristic used in the BestFirstSearch is the "costFunction". |
| costFunction  | "CircuitDepth", "PlanLength", "InstructionCost", "CircuitDepthThenLength",  "LengthThenCircuitDepth" | The cost function to use to compare plans, used to decide if a state has been seen before in a shorter plan, and used to determine the best plan found.|
| liveCounter   | Boolean       | If a live indication of search progress should be printed during the search. |
| livePrintPlans | Integer      | If >0 then finding a new plan is announced as the search finds it. If > 1 then that plan's steps are also printed. |
| quiet         | Boolean       | when set to false Cain will not announce the search starting and stopping or print search statistics. |
| initialMaxDepth | Integer     | The initial maximum number of instruction allowed in a plan before giving up on finding a valid plan. |
| forcedDepthReduction| Integer | When a new valid plan is found the MaxDepth is set to the lenght of that plan minus this value. | 
| initialMaxCost | Integer      | The intial MaxCost, the  highest cost a plan is allowed. more costly plans are not searched. |
| forcedCostReduction | Integer | When a new valid plan is found the MaxCost is set to the cost of that plan minus this value. |
| allowableAtomsCoefficient | Integer | States with more atoms than the flter we are generating multiplied by this value, plus the atoms in the inital Goals are rejected and not searched |
| goalReductionsPerStep | Integer | The number of goals that can be removed from the State in one transformation. This allows Cain to prune States with more exess goals in the goal-bag than instructions left before MaxDepth. |
| goalReductionsTolerance | Integer | This value is added to the calculation for pruning by "goalReductionsPerStep" to allow for a lower "goalReductionsPerStep" if only a few instructions might reduce the number of goals by more than "goalReductionsPerStep". | 

#### PairGen

This defines what PairGeneration to use and so what archatecture to target and what huristics to use. Pair Generation is dependant on the Goal-System and currently only Scamp5 (for Atom goals) is supported.
```
"pairGen":{
    "name": "Scamp5",
    "configGetter": "Threshold",
    "ops":"all",
    "threshold":10
  }
  ```
 
 The only configGetter implmented is "threshold" which uses the "threshold" value to determine if exhustive or atom-distance-based Scamp5 pair generation should be used. If the current state goal-bag has more than "threshold" atoms in it, in total, then atom-distance-bsed pairgeneration is used.
 
 "ops" can be set to "basic" or "all" to select between using only only: `movx`, `add`, `sub`, and `div`; or using all analogue macro instructions defined in the Scamp5 API. 




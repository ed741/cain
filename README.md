# Cain
## Convolutional-Filter Code-Generator for Cellular-Processor-Arrays


![](https://github.com/ed741/cain/workflows/Java%20CI%20with%20Maven/badge.svg)

## Installation
 1. `git clone https://github.com/ed741/cain.git`
 2. `cd cain`
 3. `mvn install`
 
 Installing using Maven will run tests and so should confirm that Cain is working.
 
## Using Cain

Cain uses a JSON input format to define the filter and search parameters to use. Examples can be found in `./examples`.

##### To run an example use:

    java -jar target/cain-3.1.jar examples/sobel.json

## JSON Format
The primary way to produce code is to specify a search using JSON. It is of course possible to use Cain like a library
and configure it how you like but for non-automated, or trivially automated use the Json format is likely to be most
helpful. Tables like the one below show what JSON tags can be specified, and if there is no default then they must be
specified. Where the valid value is itself an object that has multiple implementation options we use the "name" tag to 
specify the implementation. 

This is the [sobel example](examples/sobel.json)
```
{"name":"Sobel",
  "target": "scamp5",
  "mode": "analogue",
  "goalSystem":"atom",
  
  "runConfig":{
    "searchTime":1000,
    "timeOut":true,
    "workers":4,
    "traversalAlgorithm":"CGDS",
    "costFunction":"CircuitDepthThenLength",
    "liveCounter":true,
    "livePrintPlans":2,
    "quiet": false,
    "initialMaxDepth":200,
    "forcedDepthReduction":1,
    "initialMaxCost":2147483647,
    "forcedCostReduction":0,
    "allowableAtomsCoefficient":2,
    "goalReductionsPerStep":1,
    "goalReductionsTolerance":1
  },

  "maxApproximationDepth":3,
  "maxApproximationError":0,
  "filter":{
    "A": {"depth":0,
      "array":
      [ [1, 0, -1],
        [2, 0, -2],
        [1, 0, -1]
      ]}
  },

  "registerAllocator": {
    "name": "linearScan",
    "availableRegisters":["A","B","C","D","E","F"],
    "initialRegisters":["A"]
  },

  "pairGen":{
    "strategy": {
      "name":"Threshold",
      "threshold":10,
      "heuristic": {"name":"Pattern"},
      "ops": "all",
      "above": {
        "name":"AtomDistance"
      }
    },
    "outputFormat": {
      "name": "defaultFormat"
    }
  },

  "verifier":"Scamp5Emulator"

}
```

| token id          | valid value   | Default | Description  |
| ----------------- |:-------------:|:-------:| ------------ |
| id                | Any String    |         | The arbitary name of this search configuration. |
| verbose           | Integer       | 10      | This value controls the verbosity of the setup and checking of the search, not the search itself (see 'runConfig' for search verbosity rules) less than 0 means no output, 0 means critical only, and so on, more than 10 will print debug information. |
| target            |               |         | This determines the target architecture as well as fundamental things such as implementation of the Goals Cain will search for |
| .                 | "scamp5"      |         | target the scamp5 system, see below |
| runConfig         | RunConfig (see below) | | The run configutation for Cain, including parameters for search time, traversal algorithm, and cost function. |

### RunConfig
The RunConfig defines the behaviour of the Reverse Search Algorithm, but not any of the Pair Generation or Huristics.

| token id      | valid value   | Description  |
| ------------- |:-------------:| ------------ |
| searchTime    | Integer       | The timeout for searching|
| timeOut       | Boolean       | Whether or not Cain should timeout when searching. If Cain is set to use more than 1 worker it will never stop by itself, even if all nodes have been explored |
| workers       | Integer       | The number for worker threads to use (worker threads search semi-independently using different instances of the same traversal algorithm). This does not effect multi-threading performed within the pairGeneration invocation of a worker thread |
| traversalAlgorithm |  | The traversal Algorithm to use. CGDS is recommended. |
| .             | "CGDS"         | Child-Generator-Deque-Search - The recommended algorithm |
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

### target:"scamp5"
If target is "scamp5" then the "mode" tag must be present:

| token id   | valid value   | Default | Description  |
| ---------- |:-------------:|:-------:| ------------ |
| mode       |               |         | Specifies the paradigms and features to use, to produce code for the scamp5 device |
| .          | "analogue"    |         | Use the scamp5's analogue functionality to compute a convolutional filter |
| .          | "digital"     |         | Use the scamp5's digital functionality to compute a convolutional filter (doesn't support negative numbers) |
| .          | "superpixel"  |         | Use the scamp5's digital functionality, where multiple Processing element form a single 'super pixel' to compute a convolutional filter |
| registerAllocator | RegisterAllocator (see below) | | The registerAllocator to use.
| filter     | Filter (see below) |    | The mapping from Registers to the kernels to be compiled. |
| maxApproximationDepth | Integer |    | The maximum number of divisions by 2 that we allow when approximating the filter for binary encoding. Aka the resolution of the approximation, a value of 5 would mean the filter weights are approximate no closer than to the closest 1/32nd. |
| maxApproximationError | Double  |    | The allowable cumulative error of the approximation of a filter. A lower error means higher accuracy of results but if the weights are not simple fractions with binary number denominators then program length will increase significantly. |
| pairGen           | PairGen (see below) |   | The pair generation configuration, including any configuration parameters to give the Generator to specify things like bit-layout. |
| verifier   |               |         | The verifier to use to ensure the produced code is correct. |
| .          | "none"        |         | Do no verification. |
| .          | "Scamp5Emulator" |      | Verify using a Scamp5 Emulator, only avalable if mode is "analogue" |

#### mode:
If mode is "analogue" or "digital" then must also select a goalSystem:

| token id   | valid value   | Default | Description  |
| ---------- |:-------------:|:-------:| ------------ |
| goalSystem |               |         | Specifies the implementation used to store 'Goals' representing the convolutional kernels |
| .          | "atom"        |         | This uses a list of atoms to represent a kernel, based on Debrunner's [AUKE](https://dl.acm.org/doi/abs/10.1145/3291055) ([code](https://github.com/najiji/auto_code_cpa)) |
| .          | "array"       |         | This uses an array of weights instead, providing the same functionality with better performance, and is recommend |

When mode is "superpixel" an adapted implementation of the "array" implementation is always used.

#### RegisterAllocator
The Register allocator defines the registers Cain can use as well as the algorithm to allocate them, but there is some
nuance to them depending on the mode selected.
* If the mode is "analogue" then registers, specified as strings are directly the output registers to be used in the 
  output code
* If the mode is "digital" then the registers specified here are 'virtual registers' since each one will be converted
  into the multiple binary registers (one per bit) then used in instruction selection.
* If the mode is "superpixel", the registers are written as "0:R1" where "0" is the 'bank' and "R1" is the actual name
  of the register in the architecture. A super pixel may have multiple banks of registers: [0..].
  
#####registerAllocator.name:"linearScan"
The only register allocation algorithm implemented is linear-scan, though the implementation when mode is "superpixel"
is adapted to account for banks .

| token id          | valid value   | Default | Description  |
| -------------     |:-------------:|:-------:| ------------ |
| availableRegisters | Array of Strings | | The available Registers to use in computation (registers in the 'filter' mapping are added automatically if not present' |
| initialRegisters | Array of Strings |   | In the same format as 'availableRegisters' this defines the input registers. For single channel inputs use a singleton array, for multichannel filters the order of registers order of channels described in the innermost array of the kernel. |

if mode is "superpixel" you can alternatively use an Array or Arrays of Strings for available registers, the indices of
the outermost array become the banks of the registers.
For example `[["R2","R3"],["R2"]]` is equivalent to `["0:R2","0:R3","1:R2"]`. 


#### Filter 
The Filter JSON object is a mapping from Register to the kernel to generate in that register. Each kernel object has an
"array" of the weights, and an optional Doubles "depth" and "scale". Every weight in the kernel is multiplied by
`scale * (2^depth)`. Default values for scale and depth are 1 and 0 respectively. Weights are interpreted as Doubles;
scale and depth are just used to make reading kernels easier.

**Example**:
```
  "filter":{
    "A": {"depth":-6,
      "array":scale
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
This Filter will compute an approximated 5x5 Gaussian filter into 'A' from channel 0 as well as a 3x3 guassian filter
on channels 0 and 1 added together into Register 'B'.

#### PairGen

This defines what Generator to use and configuration options. This is heavily dependent on the mode selected. The
"strategy" is dependent on the mode and specifies the implementation used to produce new GoalPairs, though currently all
the scamp5 modes offer an equivalent set of options. There are two parts to the configuration options Static and
Dynamic. Static configurations are those that are set once inside "pairGen". Dynamic Configurations are those that we
can set in "pairGen" and then redefine inside a strategy; strategies inherit the configurations from the parent scope,
in the simple case the only parent scope is PairGen.

The "outputFormat" is a common static configuration option between all modes and so is included here:

| token id      | valid value   |  Description  |
| ------------- |:-------------:|  ------------ |
| strategy      | Strategy      | The selected pairGeneration strategy, see below for various options |
| outputFormat  |               | The particular code style to produce |
| .             | DefaultFormat | The standard format, based on the [SCAMP5d api](https://personalpages.manchester.ac.uk/staff/jianing.chen/scamp5d_lib_doc_html/index.html) |
| .             | JssFormat     | A format for a FPSP [Simulator](https://github.com/JamalMulla/Simulator) |

#####outputFormat.name:"defaultFormat"
This output format requires no extra information so ```outputFormat:{name:"defaultFormat"}``` will work.

#####outputFormat.name:"jssFormat"
This output format requires  the following options:

| token id      | valid value   | Description  |
| ------------- |:-------------:| ------------ |
| simulatorName | any string    | The name of the simulator in the output Jss simulator source code |

##### Static Configuration (mode:"analogue")
There are no addition configurations required for the Analogue mode. 
Example:
```
"pairGen":{
    "strategy": {
      "name":"Threshold",
      "threshold":10,
      "heuristic": "Pattern",
      "ops": "all"
    },
    "outputFormat": {
      "name": "defaultFormat"
    }
  }
  ```
##### Static Configuration (mode:"digital")
To produce code for the 1-bit registers in Scamp5 we must decide how the virtual registers (defined above) translate into physical registers.
When using the digital mode the following json options must be supplied:

| token id      | valid value   | Description  |
| ------------- |:-------------:| ------------ |
| bits          | Integer          | the number of bits per value stored. Scamp5 doesn't have that many physical registers but we can always compile as if we had more for the science |
| scratchRegs   | Array of Strings    | Physical Registers not used for kernel outputs or values but to facilitate operations |
| regMapping    | Map of String to Array of String | A mapping from at least all the virtual registers defined in the Register Allocator to the list of physical 1-bit registers that will hold the values from least to most significant |

Example:
```
"pairGen":{
    "strategy": {
      "name":"Threshold",
      "threshold":8,
      "heuristic": "Pattern"
    },
    "bits":2,
    "scratchRegs":["S1", "S2", "S3", "S4", "S5"],
    "regMapping":{
      "A":["DA1","DA2"],
      "B":["DB1","DB2"],
      "C":["DC1","DC2"],
      "D":["DD1","DD2"]
    },
    "outputFormat": {
      "name": "defaultFormat"
    }
  }
  ```
##### Static Configuration (mode:"superpixel")
To produce code for using super pixels in scamp5 we must define the size, shape, and layout of a superpixel. We must
also define several other physical registers available in scamp5 that control masking and directions for communication.

| token id      | valid value   | Description  |
| ------------- |:-------------:| ------------ |
| scratchRegs   | Array of Strings | Physical Registers not used for kernel outputs or values but to facilitate operations |
| selectReg     | String        | The (read-only) select register who's value is set by the "selectPattern" command |
| maskReg       | String        | The mask register that unless set blocks writes to the masked register |
| maskedReg     | String        | The masked register that cannot be written to unless the mask register is set true  |
| northReg      | String        | The direction register that enables reading from the north |
| eastReg       | String        | The direction register that enables reading from the east |
| southReg      | String        | The direction register that enables reading from the south |
| westReg       | String        | The direction register that enables reading from the west |
| width         | Integer       | The width of the superpixel to use. width = 2^k for some natural number k must hold |
| height        | Integer       | The height of the superpixel to use. height = 2^k for some natural number k must hold |
| bitOrder      | 3D array of Integers | This defines the positions of bits of banks within the super pixel. The array must have the shape: (banks, height, width), banks being at least the number of banks defined in the Register Allocator. Pad the array with 0s then use the numbers [1..] for the bits from least to most significant.|
| regMapping    | Map of String to Array of String | A mapping from at least all the virtual registers defined in the Register Allocator to the list of physical 1-bit registers that will hold the values from least to most significant |

Example:
```
 "pairGen":{
    "strategy": {
      "name":"Threshold",
      "threshold":30,
      "heuristic": "Pattern",
      "ops": "all"
    },
    "scratchRegs": ["R[scratch1]", "R[scratch2]", "R[scratch3]"],
    "selectReg": "R[select]",
    "maskReg": "R[mask]",
    "maskedReg": "R[masked]",
    "northReg": "R[north]",
    "eastReg": "R[east]",
    "southReg": "R[south]",
    "westReg": "R[west]",

    "width": 4,
    "height": 4,

    "bitOrder": [[[ 1,  2,  0,  0],
                  [ 4,  3,  0,  0],
                  [ 0,  0,  0,  0],
                  [ 0,  0,  0,  0]],

                 [[ 0,  0,  1,  2],
                  [ 0,  0,  4,  3],
                  [ 0,  0,  0,  0],
                  [ 0,  0,  0,  0]]
              ],
    "outputFormat": {
      "name": "defaultFormat"
    }
  }
```
In this example we see that 2 banks are defined. For Cain to find solutions effectively, currently, every bank must be
the same shape. The bits must be adjacent to their next more and less significant bits. The total shape of a bank 
(the non-zeros in the bitOrder) should be rectangles of size (w,h) such that w and h are binary numbers (1,2,4 etc) and
the offset of this rectangle from the edge of the superpixel (x,y) should also be a binary number (0,1,2,4 etc).

Other Useful bit Orders for varying numbers of bits  on a 4 by 4 superpixel include:

```
 |  4 bit, very efficant  |  4 bit, less efficant  |  8 bit,                | 16 bit, boustrophedonic |  16 bit, spiral        |
 |  [[ 1,  0,  0,  0],    |  [[ 1,  4,  0,  0],    |  [[ 1,  8,  0,  0],    |  [[ 1,  8,  9, 16],     |  [[ 4,  3,  2,  1],    |
 |   [ 2,  0,  0,  0],    |   [ 2,  3,  0,  0],    |   [ 2,  7,  0,  0],    |   [ 2,  7, 10, 15],     |   [ 5, 14, 13, 12],    |
 |   [ 3,  0,  0,  0],    |   [ 0,  0,  0,  0],    |   [ 3,  6,  0,  0],    |   [ 3,  6, 11, 14],     |   [ 6, 15, 16, 11],    |
 |   [ 4,  0,  0,  0]]    |   [ 0,  0,  0,  0]]    |   [ 4,  5,  0,  0]]    |   [ 4,  5, 12, 13]]     |   [ 7,  8,  9, 10]]    |
```

##### Dynamic Configuration (mode:"analogue")
When using the analogue mode we may add the "ops" tag to a strategy or PairGen to tell Cain weather to limit the available
instructions or not. If "all" is selected then cain will use more complex instructions like `Sub2x()` and `Add3()`. If
"basic" is selected instructions will be limited to what is available from compilers like AUKE.

| token id      | valid value   | Description  |
| ------------- |:-------------:| ------------ |
| ops           | "all"  | use all available Analogue instructions |
| .             | "basic"  | use basic Analogue instruction as used in AUKE |


##### strategy.name:"Threshold"
This strategy is a compound-strategy, it is used to select between two different strategies. It requires the following options inside:

| token id   | valid value   | Default | Description  |
| ---------- |:-------------:|:-------:| ------------ |
| threshold  | Integer       |         |  if `max(sum(abs(v) for v in goal) for goal in currentGoalBag]) > threshold` use the 'above' strategy, else use 'below'   |
| above      | Strategy      | AtomDistanceSorted | A strategy to use |
| below      | Strategy      | Exhaustive | A strategy to use |
| heuristic  | Heuristic     |  *      | Only required if either 'above' or 'below' is default. The Heuristic to use in the default sub-strategies |

##### strategy.name:"Exhaustive"
This strategy is available for all modes and requires the following options:

| token id      | valid value   | Description  |
| ------------- |:-------------:| ------------ |
| heuristic     | Heuristic     | The Heuristic to use to sort the possible options |

##### strategy.name:"AtomDistance"
This strategy is available for all modes and requires no additional options

##### strategy.name:"AtomDistanceSorted"
This strategy is available for all modes and requires the following options:

| token id      | valid value   | Description  |
| ------------- |:-------------:| ------------ |
| heuristic     | Heuristic     | The Heuristic to use to sort the possible options |

#### Heuristics
##### Heuristic.name:"Pattern"
This is currently the only heuristic, and it requires no extra information. The heuristic considers the sizes, and
patterns between kernels to estimate cost





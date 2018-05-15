# spare-warehouse-genetic-algorithm
A Genetic Algorithm (GA) applied to optimize a spare part warehouse based off cost, size, reliability, and extent of part. Written in Java.

There are two parts to this project, the Data Generator (which populates an arbitrary list of parts in use, with reliability, cost, and size. And there is the Genetic Algorithm itself, which optimizes the ideal spares inventory based on the total number of parts possible. A penalty is applied to the Spares Warehouse if a part is required but not avaialable. The Optimization simulates real life use of the warehouse to generate a cost function based off penalties, the cost of inventory (both in size and ROI), and the benefit fo being able to make a sale on demand. Additional details and results are available in the report. 

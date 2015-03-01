# dms-worker

A utility to run data management tasks on slave servers. Tasks are distributed via a resilient queue (AWS SQS) so that they persist until a worker is free to handle them. Multiple worker machines can cooperate to take tasks.


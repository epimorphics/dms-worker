# Automation service

Take job requests from queue and perform them.

Queue is an AWS SQS queue so resilient with no extra effort required. Can have multiple workers. Requests persistent until a worker gets to them.

Actions specified using appbase action system so can link to shell scripts and can dynamically extend.

## AWS CLI notes

    aws sqs send-message --queue-url https://sqs.eu-west-1.amazonaws.com/853478862498/lds-automation --message-body "Hello world" --message-attributes '{"action" : {"DataType":"String","StringValue":"test"}, "target" : {"DataType":"String","StringValue":"target"}}'


# Automation service

Take job requests from queue and perform them.

Queue is an AWS SQS queue so is resilient. Can have multiple workers. Requests persistent until a worker accepts it.

Actions specified using appbase action system so can link to shell scripts and can dynamically extend.

## Configuration

Raw java process logs to stdout, wrapper script should direct this to a log file.

Configuration is found in /opt/dms-worker/conf/app.conf or /etc/dms-worker/app.conf

Example:

    TBD

## Notes and warnings

It is possible for queued messages to recieved, and thus executed, twice so they shold be idempotent.

In the current design if a task is accepted by a worker but then fails it will not be retried and the only notification is on the log on the worker. The issue is that we can't tell the difference between something that will fail everytime and something which was a timing glitch and will work if rerun. To get better notification include that in the action specification. If failed jobs need to a stored somewhere for replay then they need to logged explicitly.

## AWS CLI test notes

    aws sqs send-message --queue-url https://sqs.eu-west-1.amazonaws.com/853478862498/lds-automation --message-body "Hello world" --message-attributes '{"action" : {"DataType":"String","StringValue":"test"}, "target" : {"DataType":"String","StringValue":"target"}}'

    aws sqs send-message --queue-url https://sqs.eu-west-1.amazonaws.com/853478862498/lds-automation --message-body "I am the message body" --message-attributes '{"action" : {"DataType":"String","StringValue":"testFile"}}'


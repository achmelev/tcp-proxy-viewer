**Kind (Bug, Feature)

**Short Description

Drain the pipe logic when creating a data packet

**Text

Currently the tool creates one data packet for every read of the incoming data (from both direction). This can lead to a "message" 
being split up in multiple packets. To mitigate the issue implement a "drain the pipe" logic, so that the tool creates a new data packet only,
when no more data is available in the correspondung input stream.

**Status (Open, Done, Canceled)

Done
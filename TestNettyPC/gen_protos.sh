#!/bin/bash

protoc --proto_path=protos --java_out=src protos/msgdef_v1.0.proto

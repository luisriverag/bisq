package bisq.daemon.grpc;

import bisq.core.api.CoreApi;

import bisq.proto.grpc.GetVersionGrpc;
import bisq.proto.grpc.GetVersionReply;
import bisq.proto.grpc.GetVersionRequest;

import io.grpc.stub.StreamObserver;

import javax.inject.Inject;

class GrpcVersionService extends GetVersionGrpc.GetVersionImplBase {

    private final CoreApi coreApi;
    private final GrpcExceptionHandler exceptionHandler;

    @Inject
    public GrpcVersionService(CoreApi coreApi, GrpcExceptionHandler exceptionHandler) {
        this.coreApi = coreApi;
        this.exceptionHandler = exceptionHandler;
    }

    @Override
    public void getVersion(GetVersionRequest req, StreamObserver<GetVersionReply> responseObserver) {
        try {
            var reply = GetVersionReply.newBuilder().setVersion(coreApi.getVersion()).build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        } catch (Throwable cause) {
            exceptionHandler.handleException(cause, responseObserver);
        }
    }
}

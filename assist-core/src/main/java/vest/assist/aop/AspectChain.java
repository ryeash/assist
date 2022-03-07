package vest.assist.aop;

public interface AspectChain {

    Object next(InvocationImpl invocation) throws Exception;
}

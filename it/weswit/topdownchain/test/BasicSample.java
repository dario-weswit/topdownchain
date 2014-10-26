package it.weswit.topdownchain.test;
import it.weswit.topdownchain.Chain;
import it.weswit.topdownchain.ChainOutcomeListener;
import it.weswit.topdownchain.FirstStage;
import it.weswit.topdownchain.FullStage;
import it.weswit.topdownchain.LogConsumer;
import it.weswit.topdownchain.RedirectedException;
import it.weswit.topdownchain.Redirector;
import it.weswit.topdownchain.SimpleStage;
import it.weswit.topdownchain.mock.Logger;
import it.weswit.topdownchain.mock.ThreadPoolExecutor;
import it.weswit.topdownchain.redirection.PoolRedirector;
import it.weswit.topdownchain.redirection.TimerRedirector;

import java.io.IOException;
import java.util.Timer;
import java.util.concurrent.atomic.AtomicReference;

public class BasicSample {
    
    static class Socket {
        public String read() throws IOException {
            return "RUSULTATO LETTURA";
        }
        public void write(String output) throws IOException {
            System.out.println(output);
        }
        public void close() {
        }
    }
    
    static class Request {
        private final String msg;
        public Request(String msg) {
            this.msg = msg;
        }
        public long getDelay() {
            return 1000;
        }
        public static Request parse(String input) throws ParseException {
            return new Request(input);
        }
    }
    
    public static class Response {
        private final String msg;
        public Response(String msg) {
            this.msg = msg;
        }
        public String getText() {
            return msg;
        }
    }
    
    static class Service {
        public Response serve(Request request) throws RequestException, ServiceException {
            return new Response(request.msg);
        }
        public static Service getService() {
            return new Service();
        }
    }
    
    public static class RequestException extends Exception {
        public RequestException() {
        }
    }

    public static class ParseException extends Exception {
        public ParseException() {
        }
    }

    public static class ServiceException extends Exception {
        public ServiceException(String msg) {
            super(msg);
        }
    }
    
    static void sleep(long delay) {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.interrupted();
        }
    }
    
    static void log(String msg) {
        System.out.println(msg);
    }

    static void log(Exception e) {
        System.out.println(e);
    }
    
    static ThreadPoolExecutor myPool;
    
    static class MyLogger extends Logger implements LogConsumer {}

    static MyLogger myLogger = new MyLogger();
    
    static class ReadRedirector extends Redirector.With1<IOException> {
        private final Socket socket;
        private final AtomicReference<String> inputContainer = new AtomicReference<String>();
        public ReadRedirector(Socket socket) {
            this.socket = socket;
        }
        protected void launch() {
            try {
                inputContainer.set(socket.read());
                super.onCompleted();
            } catch (IOException e) {
                super.onException1(e);
            }
        }
        public AtomicReference<String> getInputContainer() {
            return inputContainer;
        }
    }

    static class WriteRedirector extends Redirector.With1<IOException> {
        private final Socket socket;
        private final String output;
        public WriteRedirector(Socket socket, String output) {
            this.socket = socket;
            this.output = output;
        }
        protected void launch() {
            try {
                socket.write(output);
                super.onCompleted();
            } catch (IOException e) {
                super.onException1(e);
            }
        }
    }

    // launch:
    /**/ public static void main(String[] args) { /**/
        /**/ final Socket socket = new Socket(); /**/

        /**/ // /**/ ....
        Chain.startChain(
            new FirstStage() {
                public void invoke(Chain chain) throws RedirectedException {
                    ConnectionHandler.ReadWriteStage readWriteStage = ConnectionHandler.getInstance().getReadWriteStage();
                    readWriteStage.invoke(socket, chain);
                }
            },
            new ChainOutcomeListener() {
                public void onClose(Throwable thrown) {
                    if (thrown != null) {
                        /**/ System.out.println(thrown); // /**/ ....
                    }
                }
            },
            myLogger
        );

    /**/ } /**/ 
    
    public /**/ static /**/ class ConnectionHandler {
        private static ConnectionHandler instance = new ConnectionHandler();
        public static ConnectionHandler getInstance() {
            return instance;
        }

        private static final PoolRedirector parsingPool = new PoolRedirector(myPool);
        
        public static abstract class ReadWriteStage extends FullStage {
            private final AtomicReference<String> outputContainer = new AtomicReference<String>();
                // contains the answer from the sub-Stage, hence this Stage implementation cannot be shared
            @StageBody
            public void body(Socket socket, Chain chain) throws IOException, RedirectedException {
                ReadRedirector reader = new ReadRedirector(socket);
                chain.setRedirector(reader);
                answeringStage.invoke(reader.getInputContainer(), outputContainer, chain);
            }
            @StageInvocation
            public void invoke(Socket socket, Chain chain) throws RedirectedException {
                try {
                    body(socket, chain);
                    WriteRedirector writer = new WriteRedirector(socket, outputContainer.get());
                    chain.redirectAndClose(writer);
                } catch (IOException e) {
                    log(e);
                } finally {
                    socket.close();
                }
            }
        }

        public static abstract class AnsweringStage extends FullStage {
            @StageBody
            public void body(AtomicReference<String> inputContainer, AtomicReference<String> outputContainer, Chain chain)
                    throws RequestException, ServiceException, ParseException, RedirectedException
            {
                ProtocolHandler.ParsingStage parsingStage = ProtocolHandler.getInstance().getParsingStage();
                chain.setRedirector(parsingPool);
                parsingStage.invoke(inputContainer.get(), outputContainer, chain);
            }
            @StageInvocation
            public void invoke(AtomicReference<String> inputContainer, AtomicReference<String> outputContainer, Chain chain)
                    throws IOException, RedirectedException
            {
                try {
                    body(inputContainer, outputContainer, chain);
                } catch (RequestException | ParseException e) {
                    outputContainer.set("request error: " + e.getMessage());
                } catch (ServiceException e) {
                    outputContainer.set("service error: " + e.getMessage());
                }
            }
        }

        public ReadWriteStage getReadWriteStage() {
            return ReadWriteStage.getStage(ReadWriteStage.class); // cannot be shared
        }

        private static final AnsweringStage answeringStage = AnsweringStage.getStage(AnsweringStage.class); // stateless: can be shared
    }
    
    public /**/ static /**/ class ProtocolHandler {
        private static ProtocolHandler instance = new ProtocolHandler();
        public static ProtocolHandler getInstance() {
            return instance;
        }

        public static abstract class ParsingStage extends FullStage {
            private final AtomicReference<Response> responseContainer = new AtomicReference<Response>();
                // contains the answer from the sub-Stage, hence this Stage implementation cannot be shared
            @StageBody
            public void body(String input, AtomicReference<String> outputContainer, Chain chain)
                throws RequestException, ServiceException, ParseException, RedirectedException
            {
                Request request = Request.parse(input); // throws ParseException
                long delay = request.getDelay();
                RequestHandler.ExecutionStage executionStage = RequestHandler.getInstance().getExecutionStage();
                if (delay > 0) {
                    TimerRedirector timer = new TimerRedirector(/**/ new Timer(true), /**/ delay);
                    chain.setRedirector(timer);
                    executionStage.invoke(request, responseContainer, chain);
                } else {
                    executionStage.invoke(request, responseContainer, chain);
                }
            }
            @StageInvocation
            public void invoke(String input, AtomicReference<String> outputContainer, Chain chain)
                throws RequestException, ServiceException, ParseException, RedirectedException
            {
                body(input, outputContainer, chain);
                Response response = responseContainer.get();
                outputContainer.set(response.getText());
            }
        }

        public ParsingStage getParsingStage() {
            return ParsingStage.getStage(ParsingStage.class); // cannot be shared
        }
    }

    public /**/ static /**/ class RequestHandler {
        private static RequestHandler instance = new RequestHandler();
        public static RequestHandler getInstance() {
            return instance;
        }

        public static abstract class ExecutionStage extends SimpleStage {
            @StageInvocation
            public void invoke(Request request, AtomicReference<Response> responseContainer, Chain chain)
                    throws RequestException, ServiceException, RedirectedException
            {
                Service service = Service.getService();
                if (service != null) {
                    ServiceRedirector redirector = new ServiceRedirector(service, request, responseContainer);
                    chain.redirectAndClose(redirector);
                } else {
                    throw new ServiceException("Service unavailable");
                }
            }
        }
        
        private static class ServiceRedirector extends Redirector.With2<RequestException, ServiceException> {
            private final Service service;
            private final Request request;
            private final AtomicReference<Response> responseContainer;
            public ServiceRedirector(Service service, Request request, AtomicReference<Response> responseContainer) {
                this.service = service;
                this.request = request;
                this.responseContainer = responseContainer;
            }
            protected void launch() {
                try {
                    Response serviceResponse = service.serve(request); // throws RequestException, ServiceException
                    responseContainer.set(serviceResponse);
                    super.onCompleted();
                } catch (RequestException e) {
                    super.onException1(e);
                } catch (ServiceException e) {
                    super.onException2(e);
                }
            }
        }

        private final ExecutionStage executionStage = ExecutionStage.getStage(ExecutionStage.class); // stateless: can be shared

        public ExecutionStage getExecutionStage() {
            return executionStage;
        }
    }

    private static class Temp {
        // launch:
            /**/ { /**/
            /**/ // /**/ ......
            try {
                /**/ Socket socket = null; /**/
                ConnectionHandler.getInstance().process(socket);
            } catch (Throwable t) {
                /**/ System.out.println(t); // /**/ ....
            }
            /**/ } /**/

            public /**/ static /**/ class ConnectionHandler {
            private static ConnectionHandler instance = new ConnectionHandler();
            public static ConnectionHandler getInstance() {
                return instance;
            }
            public void process(Socket socket) {
                try {
                    String input = socket.read(); // throws IOException
                    try {
                        String output = ProtocolHandler.getInstance().elaborate(input);
                        socket.write(output); // throws IOException
                    } catch (RequestException | ParseException e) {
                        socket.write("request error: " + e.getMessage()); // throws IOException
                    } catch (ServiceException e) {
                        socket.write("service error: " + e.getMessage()); // throws IOException
                    }
                } catch (IOException e) {
                    log(e);
                } finally {
                    socket.close();
                }
            }
        }

        public /**/ static /**/ class ProtocolHandler {
            private static ProtocolHandler instance = new ProtocolHandler();
            public static ProtocolHandler getInstance() {
                return instance;
            }
            public String elaborate(String input) throws ParseException, RequestException, ServiceException {
                Request request = Request.parse(input); // throws ParseException
                long delay = request.getDelay();
                if (delay > 0) {
                    sleep(delay);
                }
                Response response = RequestHandler.getInstance().execute(request); // throws RequestException
                return response.getText();
            }
        }

        public /**/ static /**/ class RequestHandler {
            private static RequestHandler instance = new RequestHandler();
            public static RequestHandler getInstance() {
                return instance;
            }
            public Response execute(Request request) throws RequestException, ServiceException {
                Service service = Service.getService();
                if (service != null) {
                    return service.serve(request); // throws RequestException, ServiceException
                } else {
                    throw new ServiceException("Service unavailable");
                }
            }
        }
    }

}




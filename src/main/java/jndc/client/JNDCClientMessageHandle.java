package jndc.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoop;
import io.netty.channel.SimpleChannelInboundHandler;
import jndc.core.NDCMessageProtocol;
import jndc.core.UniqueBeanManage;
import jndc.core.config.ClientConfig;
import jndc.core.config.UnifiedConfiguration;
import jndc.core.message.RegistrationMessage;
import jndc.core.message.UserError;
import jndc.utils.InetUtils;
import jndc.utils.LogPrint;
import jndc.utils.ObjectSerializableUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

public class JNDCClientMessageHandle extends SimpleChannelInboundHandler<NDCMessageProtocol> {
    private  final Logger logger = LoggerFactory.getLogger(getClass());

    private JNDCClient client;

    public static final String NAME = "NDC_CLIENT_HANDLE";

    private ChannelHandlerContext ctx;

    private volatile boolean reConnectTag = true;

    public JNDCClientMessageHandle(JNDCClient jndcClient) {
        this.client = jndcClient;
    }

    public void sendRegisterToServer(int localPort, int serverPort) {
        RegistrationMessage registrationMessage = new RegistrationMessage();
        registrationMessage.setEquipmentId(InetUtils.uniqueInetTag);
        byte[] bytes = ObjectSerializableUtils.object2bytes(registrationMessage);


        NDCMessageProtocol tqs = NDCMessageProtocol.of(InetUtils.localInetAddress, InetUtils.localInetAddress, 0, serverPort, localPort, NDCMessageProtocol.MAP_REGISTER);
        tqs.setData(bytes);
        ctx.writeAndFlush(tqs);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.ctx = ctx;
        UnifiedConfiguration unifiedConfiguration = UniqueBeanManage.getBean(UnifiedConfiguration.class);
        ClientConfig clientConfig = unifiedConfiguration.getClientConfig();


        RegistrationMessage registrationMessage = new RegistrationMessage();
        registrationMessage.setEquipmentId(InetUtils.uniqueInetTag);
        registrationMessage.setAuth(unifiedConfiguration.getSecrete() + "1");

        byte[] bytes = ObjectSerializableUtils.object2bytes(registrationMessage);


        final InetAddress remoteInetAddress = clientConfig.getRemoteInetAddress();


        if (clientConfig == null || clientConfig.getClientPortMappingList() == null) {
            logger.debug("can not load mapping config");
            return;
        }


        clientConfig.getClientPortMappingList().forEach(x -> {
            int localPort = x.getLocalPort();
            int serverPort = x.getServerPort();
            String localIp = x.getLocalIp();
            InetAddress appAddress = null;
            try {
                appAddress = InetAddress.getByName(localIp);
            } catch (UnknownHostException e) {
                logger.debug("UnknownHostException:" + localIp);
            }
            if (appAddress != null) {
                NDCMessageProtocol tqs = NDCMessageProtocol.of(remoteInetAddress, appAddress, 0, serverPort, localPort, NDCMessageProtocol.MAP_REGISTER);
                tqs.setData(bytes);
                ctx.writeAndFlush(tqs);
            }

        });
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, NDCMessageProtocol ndcMessageProtocol) throws Exception {
        Integer type = ndcMessageProtocol.getType();
        try {


            if (type == NDCMessageProtocol.TCP_DATA) {
                //todo TCP_DATA
                JNDCClientConfigCenter bean = UniqueBeanManage.getBean(JNDCClientConfigCenter.class);
                bean.addMessageToReceiveQueue(ndcMessageProtocol);
                return;
            }


            if (type == NDCMessageProtocol.TCP_ACTIVE) {
                //todo TCP_ACTIVE
                JNDCClientConfigCenter bean = UniqueBeanManage.getBean(JNDCClientConfigCenter.class);
                bean.addMessageToReceiveQueue(ndcMessageProtocol);
                return;
            }

            if (type == NDCMessageProtocol.MAP_REGISTER) {
                //todo MAP_REGISTER

                //print msg
                RegistrationMessage object = ndcMessageProtocol.getObject(RegistrationMessage.class);
                logger.debug(object.getMessage());


                //register channel,client just hold one channelHandlerContext
                JNDCClientConfigCenter bean = UniqueBeanManage.getBean(JNDCClientConfigCenter.class);
                bean.registerMessageChannel(0, channelHandlerContext);
                return;

            }

            if (type == NDCMessageProtocol.CONNECTION_INTERRUPTED) {
                //todo CONNECTION_INTERRUPTED

                JNDCClientConfigCenter bean = UniqueBeanManage.getBean(JNDCClientConfigCenter.class);
                bean.shutDownClientPortProtector(ndcMessageProtocol);
                return;
            }

            if (type == NDCMessageProtocol.NO_ACCESS) {
                //todo NO_ACCESS
                logger.debug(new String(ndcMessageProtocol.getData()));
                return;
            }

            if (type == NDCMessageProtocol.USER_ERROR) {
                //todo USER_ERROR
                UserError userError = ndcMessageProtocol.getObject(UserError.class);
                if (userError.isAuthFail()) {
                    reConnectTag = false;//not restart
                    channelHandlerContext.close();
                    channelHandlerContext.channel().eventLoop().shutdownGracefully().addListener(x -> {
                        if (x.isSuccess()) {
                            logger.debug("register auth fail, the client will close later...");
                        } else {
                            logger.debug("shutdown fail");
                        }
                    });

                }
                return;
            }

            if (type == NDCMessageProtocol.UN_CATCHABLE_ERROR) {
                //todo UN_CATCHABLE_ERROR
                logger.debug(new String(ndcMessageProtocol.getData()));
                return;
            }

        } catch (Exception e) {
            logger.debug("unCatchableError" + e);
//            NDCMessageProtocol copy = ndcMessageProtocol.copy();
//            copy.setType(NDCMessageProtocol.UN_CATCHABLE_ERROR);
//            channelHandlerContext.writeAndFlush(copy);
        }


    }


    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (reConnectTag) {
            logger.debug("client connection interrupted, will restart on 5 second later");
            TimeUnit.SECONDS.sleep(5);
            EventLoop eventExecutors = ctx.channel().eventLoop();
            client.createClient(eventExecutors);
        }

    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.debug("unCatchable client error：" + cause.getMessage());
    }

}

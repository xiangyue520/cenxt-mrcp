package cn.cenxt.cc.demo.ai.echo;

import cn.cenxt.cc.demo.ai.AiConversationManager;
import cn.cenxt.cc.demo.util.LogUtil;
import cn.cenxt.cc.mrcp.ai.AiProcessHandler;
import cn.cenxt.cc.mrcp.ai.AiRequest;
import cn.cenxt.cc.mrcp.ai.AiResultListener;
import cn.cenxt.cc.mrcp.common.StopHandle;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
public class EchoProcessor implements AiProcessHandler, InitializingBean {

    @Autowired
    private AiConversationManager conversationManager;

    @Override
    public void afterPropertiesSet() throws Exception {
    }


    @Override
    public String getName() {
        return "echo";
    }

    /**
     * LLM处理
     *
     * @param request  请求
     * @param listener 监听器
     * @return 处理句柄
     */
    @Override
    public StopHandle start(AiRequest request, AiResultListener listener) {
        log.info("coze agent query:{}", request);
        AtomicReference<String> chatId = new AtomicReference<>();
        try {
            String uid = request.getCaller();
            if (!StringUtils.hasText(uid)) {
                uid = "uid";
            }
            String query = request.getQuery();


            try {
                String content = "这是" + query + "的回答";
                LogUtil.setUuid(request.getUuid());
                log.info("coze agent query receive complete:{}", content);
                chatId.set("");
                listener.complete(content);
            } catch (Exception e) {
                log.error("coze agent query resp foreach error", e);
                chatId.set("");
                listener.error(e);
            }
        } catch (Exception e) {
            log.error("coze agent query error", e);
            chatId.set("");
            listener.error(e);

        }
        return new StopHandle() {
            /**
             * 停止TTS处理
             */
            @Override
            protected void onStop() {
                log.info("echo agent query stop");
                String id = chatId.get();
                String conversation = conversationManager.getConversation(request.getUuid());
                int count = 0;
                while (id == null) {
                    id = chatId.get();
                    count++;
                    if (count > 10) {
                        break;
                    }
                    try {
                        log.info("coze agent close wait for chatId");
                        Thread.sleep(200);
                    } catch (InterruptedException ignored) {
                    }
                }
                if (id == null || id.isEmpty()) {
                    return;
                }
                log.info("coze agent send cancel conversationId:{}, chatId:{}", conversation, id);
                //try {
                //    CancelChatReq req = CancelChatReq.builder()
                //            .chatID(id)
                //            .conversationID(conversation)
                //            .build();
                //
                //    CancelChatResp cancel = coze.chat().cancel(req);
                //    log.info("coze agent query cancel resp:{}", JSON.toJSONString(cancel));
                //} catch (Exception e) {
                //    log.info("coze agent query cancel error:{}", e.getMessage());
                //}
                listener.complete(null);
            }
        };
    }
}

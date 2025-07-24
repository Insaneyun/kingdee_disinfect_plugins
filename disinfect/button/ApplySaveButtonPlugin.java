package plugins.disinfect.button;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.operate.interaction.InteractionContext;
import kd.bos.entity.operate.interaction.KDInteractionException;
import kd.bos.entity.operate.result.OperateErrorInfo;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.args.BeginOperationTransactionArgs;
import kd.bos.entity.validate.ErrorLevel;
import kd.bos.servicehelper.AttachmentServiceHelper;
import kd.sdk.plugin.Plugin;

import java.util.List;
import java.util.Map;

/**
 * 单据操作插件
 * 判断附件是否为空
 */
public class ApplySaveButtonPlugin extends AbstractOperationServicePlugIn implements Plugin {

    private static final String APPLY = "smk5_staffsterilize";

    private static final String ATTACHMENT = "attachmentpanel";



    @Override
    public void beginOperationTransaction(BeginOperationTransactionArgs e) {
        super.beginOperationTransaction(e);
        DynamicObject[] entities = e.getDataEntities();


        if(entities.length > 0){
            for(DynamicObject obj : entities){

                    List<Map<String, Object>> attachments = AttachmentServiceHelper.getAttachments(APPLY, obj.getPkValue(), ATTACHMENT);
                if(attachments.isEmpty()){
                    InteractionContext interactionContext = new InteractionContext();
                    OperateErrorInfo errorInfo = new OperateErrorInfo();
                    errorInfo.setMessage("附件不能为空，请添加");
                    errorInfo.setLevel(ErrorLevel.Error);
                    interactionContext.addOperateInfo(errorInfo);
                    throw new KDInteractionException("custom.interaction.key",interactionContext);
                }
            }
        }
    }
}
package plugins.disinfect.task;

import dm.jdbc.util.StringUtil;
import kd.bos.context.RequestContext;
import kd.bos.dataentity.OperateOption;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.entity.LocaleString;
import kd.bos.dataentity.utils.StringUtils;
import kd.bos.entity.operate.result.OperationResult;
import kd.bos.exception.KDException;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.schedule.executor.AbstractTask;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import kd.bos.servicehelper.workflow.MessageCenterServiceHelper;
import kd.bos.workflow.engine.msg.info.MessageInfo;
import kd.bos.workflow.message.service.MessageCenterService;
import kd.sdk.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 后台任务插件
 */
public class DisinfectRecordTimeTask extends AbstractTask implements Plugin {
    private static final String ALLOWED = "D";
    private static final String DISINFECTING = "E";
    private static final String THROWED = "G";


    private static final String DISINFECT_APPLY = "smk5_staffsterilize";
    private static final String DISINFECT_APPLY_APPLICANT = "smk5_people.id";
    private static final String DISINFECT_APPLY_STATUS = "billstatus";


    @Override
    public void execute(RequestContext requestContext, Map<String, Object> map) throws KDException {
        long currUserId = requestContext.getCurrUserId();
        DynamicObjectCollection pks = QueryServiceHelper.query(
                DISINFECT_APPLY,
                "id",
                new QFilter[]{new QFilter(DISINFECT_APPLY_APPLICANT, QCP.equals, currUserId)});
        DynamicObjectCollection dynamicObjects = new DynamicObjectCollection();
        for (DynamicObject pkObj : pks) {

            Long pk = (Long) pkObj.get(0);
            DynamicObject apply = BusinessDataServiceHelper.loadSingle(pk, DISINFECT_APPLY);
            String status = (String) apply.get(DISINFECT_APPLY_STATUS);
            int result = status.compareTo(DISINFECTING);
            if (result < 0) {
                createAndSendNewMessageInfo(pkObj);
                apply.set(DISINFECT_APPLY_STATUS, THROWED);// 改为已废弃
                dynamicObjects.add(apply);

            }
        }

        SaveServiceHelper.saveOperate(DISINFECT_APPLY, dynamicObjects.toArray(new DynamicObject[0]), OperateOption.create());

    }

    /**
     * 亲爱的棕熊工厂员工XX，你所提交的XX时间进入XX车间的申请单已超期并自动关闭，如还需进入车间，请重新发起申请！
     * @param pk
     * @return
     */
    private static void createAndSendNewMessageInfo(DynamicObject pk){
        DynamicObjectCollection baseInfos = QueryServiceHelper.query(
                DISINFECT_APPLY,
                "smk5_people.name,smk5_datefield1,smk5_orgfield2.name,smk5_people.id",
                new QFilter[]{new QFilter("id", QCP.equals, pk.get(0))}
        );
        DynamicObject baseInfo = baseInfos.get(0);
        MessageInfo mes = new MessageInfo();
        LocaleString title = new LocaleString();
        title.setLocaleValue_zh_CN("员工消毒申请已过期");
        mes.setMessageTitle(title);
        LocaleString content = new LocaleString();
        content.setLocaleValue_zh_CN("亲爱的棕熊工厂员工"+baseInfo.get(0)+"，你所提交的"+baseInfo.get(1)+"时间进入"+baseInfo.get(2)+"车间的申请单已超期并自动关闭，如还需进入车间，请重新发起申请！");
        mes.setMessageContent(content);
        List<Long> ids = new ArrayList<>();
        ids.add((Long)baseInfo.get(3));
        mes.setUserIds(ids);

        mes.setTag("MSGTEST");
        mes.setType("message");

        MessageCenterServiceHelper.sendMessage(mes);

    }
}
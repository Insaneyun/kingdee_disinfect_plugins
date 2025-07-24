package plugins.disinfect;

import akka.remote.artery.InboundControlJunction;
import kd.bos.context.RequestContext;
import kd.bos.dataentity.OperateOption;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.entity.LocaleString;
import kd.bos.dataentity.utils.StringUtils;
import kd.bos.entity.BadgeInfo;
import kd.bos.entity.datamodel.ListSelectedRow;
import kd.bos.entity.datamodel.ListSelectedRowCollection;
import kd.bos.entity.operate.result.OperationResult;
import kd.bos.form.control.Toolbar;
import kd.bos.form.control.events.BeforeItemClickEvent;
import kd.bos.form.control.events.ItemClickEvent;
import kd.bos.form.events.BeforeCreateListColumnsArgs;
import kd.bos.form.events.BeforeCreateListDataProviderArgs;
import kd.bos.form.events.SetFilterEvent;
import kd.bos.list.IListView;
import kd.bos.list.ListColumn;
import kd.bos.list.events.ListRowClickEvent;
import kd.bos.list.plugin.AbstractListPlugin;
import kd.bos.mvc.list.ListDataProvider;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.AttachmentServiceHelper;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.servicehelper.coderule.CodeRuleServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import kd.bos.servicehelper.user.UserServiceHelper;
import kd.bos.servicehelper.workflow.WorkflowServiceHelper;
import kd.bos.web.actions.utils.FilePathUtil;
import kd.sdk.plugin.Plugin;

import java.io.IOException;
import java.net.URLDecoder;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EventObject;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 标准单据列表插件
 * ⼯申请单，员⼯只能看到⾃⼰的申请单，⻋间负责⼈除了能看到⾃⼰发起的申请单，还可看
 * 到“申请进⼊⻋间”等于负责⼈所负责⻋间的⼈员申请
 */
public class PermissionVerificationListPlugin extends AbstractListPlugin implements Plugin {



    private static final String DPT_ID = "smk5_orgfield.id";

    private static final String APPLICANT_ID = "smk5_people.id";
    //开始消毒按钮
    private static final String START_DISINFECT = "smk5_baritemap1";

    private static final String REVIEWED = "D";

    private static final String TOOL_BAR = "toolbarap";

    private static final String DISINFECT_PLAN = "smk5_plan";
    private static final String DISINFECT_PLAN_ORG = "smk5_entryentity1.smk5_orgfield";
    private static final String DISINFECT_PLAN_GRADE = "smk5_level_grade.smk5_disinfect_level";
    private static final String DISINFECT_PLAN_STEP = "smk5_level_grade.smk5_subentryentity.smk5_step";


    private static final String DISINFECT_APPLY = "smk5_staffsterilize";
    private static final String DISINFECT_APPLY_ATTACH = "attachmentpanel";
    private static final String DISINFECT_APPLY_STATUS = "billstatus";



    //消毒记录单
    private static final String DISINFECT_RECORD = "smk5_sterilizebill";
    private static final String DISINFECT_RECORD_NO = "smk5_billnofield";
    private static final String DISINFECT_RECORD_APPLICANT = "smk5_userfield";
    private static final String DISINFECT_RECORD_CARROOM = "smk5_basedatafield";
    private static final String DISINFECT_RECORD_PLAN = "smk5_basedatafield1";
    private static final String DISINFECT_RECORD_TIME = "smk5_datefield";
    private static final String DISINFECT_RECORD_STATUS = "billstatus";
    private static final String DISINFECT_RECORD_ATTACH = "attachmentpanel";
    private static final String DISINFECT_RECORD_APPLYID = "smk5_applyid";
    private static final String DISINFECT_RECORD_STEP = "smk5_entryentity1";
    private static final String DISINFECT_RECORD_STEP_GRADE = "smk5_basedatafield2";
    private static final String DISINFECT_RECORD_STEP_STEP = "smk5_basedatafield3";
    private static final String DISINFECT_RECORD_STEP_STATUS = "smk5_billstatusfield1";



    /**
     * ⼯申请单，员⼯只能看到⾃⼰的申请单，⻋间负责⼈除了能看到⾃⼰发起的申请单，还可看
     * 到“申请进⼊⻋间”等于负责⼈所负责⻋间的⼈员申请
     *
     * @param e
     */
    @Override
    public void setFilter(SetFilterEvent e) {
        super.setFilter(e);
        List<QFilter> qFilters = e.getQFilters();

        long currUserId = RequestContext.get().getCurrUserId();
        List<Long> inchargeOrgs = UserServiceHelper.getInchargeOrgs(currUserId, true);
        //非普通员工
        if (inchargeOrgs.size() > 0) {
            QFilter qFilter = new QFilter(DPT_ID, QCP.in, inchargeOrgs);
            e.setCustomQFilters(Arrays.asList(qFilter));
        } else {
            QFilter qFilter = new QFilter(APPLICANT_ID, QCP.equals, currUserId);
            e.setCustomQFilters(Arrays.asList(qFilter));
        }
    }

    /**
     * 员⼯申请单，开始消毒按钮上显示徽标，徽标值为列表单据状态为已审核的单据
     */
    @Override
    public void afterBindData(EventObject e) {
        int count = 0;
        super.afterBindData(e);
        IListView listView = (IListView) this.getView();
        ListSelectedRowCollection allRowCollection = listView.getCurrentListAllRowCollection();
        for (ListSelectedRow obj : allRowCollection) {
            String billStatus = obj.getBillStatus();
            if (billStatus.equals(REVIEWED)) {
                count++;
            }
        }
        Toolbar toolbar = this.getView().getControl(TOOL_BAR);
        BadgeInfo badgeInfo = new BadgeInfo();
        badgeInfo.setCount(count);
        toolbar.setBadgeInfo(START_DISINFECT, badgeInfo);

    }

    /**
     * ⼈员申请单，开始消毒按钮，选中已审核的申请单时，按钮才可点击，否则按钮不可⽤
     *
     * @param evt
     */
    @Override
    public void listRowClick(ListRowClickEvent evt) {
        super.listRowClick(evt);
        ListSelectedRowCollection selectedRows = ((IListView) this.getView()).getSelectedRows();
        if (selectedRows.isEmpty()) {
            this.getView().setEnable(true, START_DISINFECT);
        }
        for (ListSelectedRow obj : selectedRows) {
            if (obj.getBillStatus().equals(REVIEWED)) {
                this.getView().setEnable(true, START_DISINFECT);
            } else {
                this.getView().setEnable(false, START_DISINFECT);
            }
        }
    }


    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);
//        Button button = this.getView().getControl(TOOL_BAR);
//        button.addClickListener(this);
        this.addItemClickListeners(TOOL_BAR);
    }

    /**
     * ⼈员申请单，开始消毒按钮，⾮“申请进⼊时间”当天操作提示“申请⽇期当天才能开始消
     * 毒”。⾮“申请进⼊⻋间”的负责⼈操作提示“你不是申请进⼊⻋间的负责⼈，没有权限进⾏消毒！
     *
     * @param evt
     */
    @Override
    public void beforeItemClick(BeforeItemClickEvent evt) {
        super.beforeItemClick(evt);
        if (evt.getItemKey().equals(START_DISINFECT)) {
            ListSelectedRowCollection selectedRows = ((IListView) this.getView()).getSelectedRows();
            for (ListSelectedRow obj : selectedRows) {
                Object keyValue = obj.getPrimaryKeyValue();

                DynamicObject[] load = BusinessDataServiceHelper.load(
                        DISINFECT_APPLY,
                        "smk5_datefield1,smk5_orgfield2.id",
                        new QFilter[]{new QFilter("id", QCP.equals, keyValue)});

                //校验部门负责人以及日期
                for (DynamicObject object : load) {

                    Timestamp date = (Timestamp) object.get("smk5_datefield1");
                    LocalDate datePart = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                    LocalDate today = LocalDate.now(); // 今天

                    if (!today.equals(datePart)) {
                        this.getView().showMessage("申请日期当天才能开始消毒");
                    }


                    Long orgId = (Long) object.get("smk5_orgfield2.id");
                    List<Long> managersOfOrg = UserServiceHelper.getManagersOfOrg(orgId);
                    long currUserId = RequestContext.get().getCurrUserId();
                    boolean orgVerify = false;
                    for (Long id : managersOfOrg) {
                        if (currUserId == id) {
                            orgVerify = true;
                        }
                    }
                    if (!orgVerify) {
                        this.getView().showMessage("你不是申请进入车间的负责人，没有权限进行消毒");
                    }
                }
            }
        }

    }

    /**
     * ⼈员申请单列表，实时展示单据在流程中的当前处理⼈，不在流程中或流程已结束则显示
     *
     * @param args
     */
    @Override
    public void beforeCreateListDataProvider(BeforeCreateListDataProviderArgs args) {
        super.beforeCreateListDataProvider(args);
        args.setListDataProvider(new ListDataProvider() {
            @Override
            public DynamicObjectCollection getData(int start, int limit) {
                DynamicObjectCollection rows = super.getData(start, limit);
                for (DynamicObject row : rows) {
                    String pkValue = row.getPkValue().toString();
                    boolean inProcess = WorkflowServiceHelper.inProcess(pkValue);
                    if (inProcess) {
                        List<Long> approverIds = WorkflowServiceHelper.getApproverByBusinessKey(pkValue);
                        List<Map<String, Object>> userInfos = UserServiceHelper.getUserInfoByID(approverIds);
                        List<String> names = userInfos
                                .stream()
                                .map(userInfo -> (String) userInfo.get("name"))
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());

                        String name = names.stream().collect(Collectors.joining(","));
                        row.set("smk5_process_handler", name);

                    }
                }
                return rows;
            }
        });
    }

    @Override
    public void beforeCreateListColumns(BeforeCreateListColumnsArgs args) {
        super.beforeCreateListColumns(args);
        ListColumn listColumn = new ListColumn();
        listColumn.setCaption(new LocaleString("当前处理人"));
        listColumn.setKey("smk5_process_handler");
        listColumn.setListFieldKey("smk5_process_handler");
        args.addListColumn(listColumn);
    }

    /**
     * 消毒申请单——————下推————————消毒记录单
     * @param evt
     */
    @Override
    public void itemClick(ItemClickEvent evt) {
        super.itemClick(evt);

        if(!evt.getItemKey().equals("smk5_baritemap1")){
            return;
        }
        ListSelectedRowCollection applyBills = ((IListView) this.getView()).getSelectedRows();
        List<DynamicObject> newRecords = new ArrayList<>();
        DynamicObject newRecord = new DynamicObject();

        for (ListSelectedRow applyBill : applyBills) {
            Object primaryKeyValue = applyBill.getPrimaryKeyValue();
            DynamicObject apply = BusinessDataServiceHelper.loadSingle(primaryKeyValue, DISINFECT_APPLY);

            newRecord = BusinessDataServiceHelper.newDynamicObject(DISINFECT_RECORD, false, null);
            String number = CodeRuleServiceHelper.getNumber(DISINFECT_RECORD, newRecord, null);
            long currUserId = RequestContext.get().getCurrUserId();
            DynamicObjectCollection query = QueryServiceHelper.query(
                    DISINFECT_PLAN,
                    "id,"+DISINFECT_PLAN_GRADE +","+DISINFECT_PLAN_STEP,
                    new QFilter[]{new QFilter("smk5_entryentity1.smk5_orgfield.id", QCP.equals, apply.get("smk5_orgfield2.id")),
                            new QFilter("smk5_version_status", QCP.equals, "D")});


            DynamicObjectCollection entry = newRecord.getDynamicObjectCollection(DISINFECT_RECORD_STEP);
            DynamicObject row ;
            //赋值
            newRecord.set(DISINFECT_RECORD_NO, number);
            newRecord.set(DISINFECT_RECORD_APPLICANT, currUserId);
            //申请进入车间
            newRecord.set(DISINFECT_RECORD_CARROOM, apply.get("smk5_orgfield2"));


            newRecord.set(DISINFECT_RECORD_TIME, apply.get("smk5_datefield1"));
            newRecord.set(DISINFECT_RECORD_STATUS, "E");

            newRecord.set(DISINFECT_RECORD_APPLYID,primaryKeyValue);
            // 附件上传先空着
            //给单据体赋值
            int counter = 0;
            for(DynamicObject obj : query){
                newRecord.set(DISINFECT_RECORD_PLAN, obj.get(0));
                row = entry.addNew();
                row.set(DISINFECT_RECORD_STEP_GRADE,obj.get(1));
                row.set(DISINFECT_RECORD_STEP_STEP,obj.get(2));
                row.set(DISINFECT_RECORD_STEP_STATUS,counter == 0 ? "B":"A");
                counter++;
            }

            //只有第一条步骤分录可以为进行中，后面需要手动修改


            OperationResult operationResult = SaveServiceHelper.saveOperate(DISINFECT_RECORD, new DynamicObject[]{newRecord}, OperateOption.create());
            if(!operationResult.isSuccess()){
                this.getView().showMessage("创建失败");
            }
            List<Object> successPkIds = operationResult.getSuccessPkIds();

            List<Map<String, Object>> disinfectApplyAttachments = AttachmentServiceHelper.getAttachments(DISINFECT_APPLY, primaryKeyValue, DISINFECT_APPLY_ATTACH);
            for(Map<String,Object> attach : disinfectApplyAttachments){
                try {
                    attach.put("url", getPathfromDownloadUrl(URLDecoder.decode(String.valueOf(attach.get("url")), "UTF-8")));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            AttachmentServiceHelper.upload(DISINFECT_RECORD,successPkIds.get(0),DISINFECT_RECORD_ATTACH,disinfectApplyAttachments);
            //消毒状态改为进行中
            apply.set(DISINFECT_APPLY_STATUS,"E");
            SaveServiceHelper.saveOperate(DISINFECT_APPLY,new DynamicObject[]{apply},OperateOption.create());



        }









    }

    /**
     * 获取文件服务器相对路径
     * @param url
     * @return
     * @throws IOException
     */
    private String getPathfromDownloadUrl(String url) throws IOException {
        String path = StringUtils.substringAfter(url, "path=");
        path = URLDecoder.decode(path, "UTF-8");
        return FilePathUtil.dealPath(path, "attach");
    }




}
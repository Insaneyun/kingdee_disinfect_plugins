package plugins.disinfect;

import kd.bos.base.AbstractBasePlugIn;
import kd.bos.context.RequestContext;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.form.control.EntryGrid;
import kd.bos.form.control.Toolbar;
import kd.bos.form.control.events.BeforeItemClickEvent;
import kd.bos.form.control.events.ItemClickEvent;
import kd.bos.form.field.BasedataEdit;
import kd.bos.form.field.events.BeforeF7SelectEvent;
import kd.bos.form.field.events.BeforeF7SelectListener;
import kd.bos.orm.ORM;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.servicehelper.user.UserServiceHelper;
import kd.sdk.plugin.Plugin;

import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

/**
 * 基础资料插件
 *
 *
 */
public class DisSchameFormPlugin extends AbstractBasePlugIn implements Plugin, BeforeF7SelectListener {

    /**
     * 用于初始化新增页面时，自动带出消毒等级
     */

    private static final int PURPOSE_LINES = 3;
    //消毒方案‘消毒等级’单据体名称
    private static final String LEVEL_GRDAE = "smk5_level_grade";
    //颜色配置
    private static final String colors[] = {"yellow", "blue", "red"};
    //消毒方案单据体‘消毒等级’标识
    private static final String DISINFECT_PLAN_LEVEL = "smk5_disinfect_level";
    //消毒方案单据体'消毒步骤'标识
    private static final String DISINFECT_PLAN_STEP = "smk5_step";
    //消毒等级容器标识
    private static final String DISINFECT_LEVEL = "smk5_level";
    //消毒步骤‘消毒等级’标识
    private static final String DISINFECT_STEP_LEVEL = "smk5_level";
    //发布按钮
    private static final String PUBLISH_BUTTON = "smk5_publish";

    private static final String DISINFECT_STEP = "smk5_advconap1";

    @Override
    public void registerListener(EventObject e) {
        BasedataEdit basedataEdit = this.getView().getControl(DISINFECT_PLAN_STEP);
        basedataEdit.addBeforeF7SelectListener(this);

        Toolbar tbmain = this.getView().getControl("tbmain");
        tbmain.addItemClickListener(this);

        super.registerListener(e);
    }
    @Override
    public void afterCreateNewData(EventObject e) {
        super.afterCreateNewData(e);
        this.getModel().batchCreateNewEntryRow(LEVEL_GRDAE, PURPOSE_LINES);
        EntryGrid levelEntity = this.getControl(LEVEL_GRDAE);
        DynamicObject[] disinfectLevel = BusinessDataServiceHelper.load(DISINFECT_LEVEL, "", null,"number asc");
        for (int i = 0; i < PURPOSE_LINES; i++) {
            this.getModel().setValue(DISINFECT_PLAN_LEVEL, disinfectLevel[i].get("id"), i);
            levelEntity.setRowBackcolor(colors[i], new int[]{i  });
        }
    }



    @Override
    public void beforeF7Select(BeforeF7SelectEvent beforeF7SelectEvent) {
    //    this.getModel().batchCreateNewEntryRow(DISINFECT_STEP,1);

        //获取当前选中的消毒等级
        int index = this.getModel().getEntryCurrentRowIndex(LEVEL_GRDAE);
        DynamicObject level = (DynamicObject) this.getModel().getValue(DISINFECT_PLAN_LEVEL, index);


        String name = beforeF7SelectEvent.getProperty().getName();
        if (name.equals(DISINFECT_PLAN_STEP)) {
            List<QFilter> qFilters = new ArrayList<>();
            QFilter qFilter = new QFilter(DISINFECT_STEP_LEVEL, QCP.equals, level.get("id"));
            qFilters.add(qFilter);
            beforeF7SelectEvent.setCustomQFilters(qFilters);
        }
    }


//    @Override
//    public void beforeItemClick(BeforeItemClickEvent evt) {
//
//        if(PUBLISH_BUTTON.equals(evt.getItemKey())){
//            long currUserId = RequestContext.get().getCurrUserId();
//
//            QFilter f1 = new QFilter("id", QCP.equals, currUserId);
//            f1.and("entryentity.isincharge", QCP.equals, true);
//
//            boolean exists = QueryServiceHelper.exists("bos_user", f1.toArray());
//            if (!exists) {
//                this.getView().showMessage("你不是⻋间负责⼈⽆法发布");
//            }
//        }
//
//    }


}
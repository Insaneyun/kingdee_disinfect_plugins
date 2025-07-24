package plugins.disinfect.report;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.report.ReportQueryParam;
import kd.bos.form.plugin.IFormPlugin;
import kd.bos.report.events.CellStyleRule;
import kd.bos.report.events.CreateColumnEvent;
import kd.bos.report.plugin.AbstractReportFormPlugin;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.sdk.plugin.Plugin;

import java.util.List;

/**
 * 报表界面插件
 */
public class DisinfectRecordFormPlugin extends AbstractReportFormPlugin implements Plugin, IFormPlugin {

    private static final String DISINFECT_RECORD = "smk5_sterilizebill";

    @Override
    public void setCellStyleRules(List<CellStyleRule> cellStyleRules) {
        super.setCellStyleRules(cellStyleRules);
        CellStyleRule cellStyleRule = new CellStyleRule();
        cellStyleRule.setFieldKey("smk5_roomname");
        cellStyleRule.setBackgroundColor("red");

        cellStyleRule.setCondition("smk5_year_count > 10 && smk5_roomname != '合计'");
        cellStyleRules.add(cellStyleRule);
    }





}
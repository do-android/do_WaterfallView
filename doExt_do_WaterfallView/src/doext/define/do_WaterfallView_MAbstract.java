package doext.define;

import core.object.DoUIModule;
import core.object.DoProperty;
import core.object.DoProperty.PropertyDataType;


public abstract class do_WaterfallView_MAbstract extends DoUIModule{

	protected do_WaterfallView_MAbstract() throws Exception {
		super();
	}
	
	/**
	 * 初始化
	 */
	@Override
	public void onInit() throws Exception{
        super.onInit();
        //注册属性
		this.registProperty(new DoProperty("canScrollToTop", PropertyDataType.Bool, "true", true));
		this.registProperty(new DoProperty("headerView", PropertyDataType.String, "", true));
		this.registProperty(new DoProperty("hSpacing", PropertyDataType.Number, "", true));
		this.registProperty(new DoProperty("isHeaderVisible", PropertyDataType.Bool, "false", true));
		this.registProperty(new DoProperty("items", PropertyDataType.String, "", false));
		this.registProperty(new DoProperty("numColumns", PropertyDataType.Number, "1", true));
		this.registProperty(new DoProperty("templates", PropertyDataType.String, "", true));
		this.registProperty(new DoProperty("vSpacing", PropertyDataType.Number, "", true));
	}
}
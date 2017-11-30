package doext.implement;

import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ItemDecoration;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.text.TextUtils;
import android.view.View;
import core.DoServiceContainer;
import core.helper.DoJsonHelper;
import core.helper.DoScriptEngineHelper;
import core.helper.DoTextHelper;
import core.helper.DoUIModuleHelper;
import core.interfaces.DoIListData;
import core.interfaces.DoIPage;
import core.interfaces.DoIScriptEngine;
import core.interfaces.DoIUIModuleView;
import core.object.DoInvokeResult;
import core.object.DoMultitonModule;
import core.object.DoProperty;
import core.object.DoSourceFile;
import core.object.DoUIContainer;
import core.object.DoUIModule;
import doext.define.do_WaterfallView_IMethod;
import doext.define.do_WaterfallView_MAbstract;
import doext.waterfallview.pullToRefresh.DoPullToRefreshView;

/**
 * 自定义扩展UIView组件实现类，此类必须继承相应VIEW类，并实现DoIUIModuleView,do_WaterfallView_IMethod接口；
 * #如何调用组件自定义事件？可以通过如下方法触发事件：
 * this.model.getEventCenter().fireEvent(_messageName, jsonResult);
 * 参数解释：@_messageName字符串事件名称，@jsonResult传递事件参数对象； 获取DoInvokeResult对象方式new
 * DoInvokeResult(this.model.getUniqueKey());
 */
public class do_WaterfallView_View extends DoPullToRefreshView implements DoIUIModuleView, do_WaterfallView_IMethod {

	// 参考 http://www.dayongxin.com/2016/08/12/2016081201/
	// 滚动条 http://www.th7.cn/Program/Android/201705/1168634.shtml
	// 设置间距 http://www.cnblogs.com/ibosong/p/6797367.html

	/**
	 * 每个UIview都会引用一个具体的model实例；
	 */
	private do_WaterfallView_MAbstract model;
	protected WaterfallViewAdapter myAdapter;
	int columuCount = 1;
	RecyclerView recyclerView;

	public do_WaterfallView_View(Context context) {
		super(context);
	}

	/**
	 * 初始化加载view准备,_doUIModule是对应当前UIView的model实例
	 */
	@Override
	public void loadView(DoUIModule _doUIModule) throws Exception {
		this.model = (do_WaterfallView_MAbstract) _doUIModule;
		myAdapter = new WaterfallViewAdapter(model);
		recyclerView = new RecyclerView(DoServiceContainer.getPageViewFactory().getAppContext());
		recyclerView.setItemAnimator(new DefaultItemAnimator());

		String _headerViewPath = this.model.getHeaderView();
		recyclerView.setBackgroundColor(Color.TRANSPARENT);
		setHeaderView(createHeaderView(_headerViewPath));
		this.addView(recyclerView, new LayoutParams((int) _doUIModule.getRealWidth(), (int) _doUIModule.getRealHeight()));
		this.setSupportHeaderRefresh(isHeaderVisible());
		this.onFinishInflate();

		initListener();
	}

	private DoUIContainer headerRootUIContainer;
	private String headerUIPath;

	public void loadDefalutScriptFile() throws Exception {
		if (headerRootUIContainer != null && headerUIPath != null) {
			headerRootUIContainer.loadDefalutScriptFile(headerUIPath);
		}
	}

	private boolean isHeaderVisible() throws Exception {
		DoProperty _property = this.model.getProperty("isHeaderVisible");
		if (_property == null) {
			return false;
		}
		return DoTextHelper.strToBool(_property.getValue(), false);
	}

	private View createHeaderView(String _uiPath) throws Exception {
		View _newView = null;
		if (_uiPath != null && !"".equals(_uiPath.trim())) {
			this.headerUIPath = _uiPath;
			DoIPage _doPage = this.model.getCurrentPage();
			DoSourceFile _uiFile = _doPage.getCurrentApp().getSourceFS().getSourceByFileName(_uiPath);
			if (_uiFile != null) {
				headerRootUIContainer = new DoUIContainer(_doPage);
				headerRootUIContainer.loadFromFile(_uiFile, null, null);
				if (null != _doPage.getScriptEngine()) {
					headerRootUIContainer.loadDefalutScriptFile(_uiPath);
				}
				DoUIModule _model = headerRootUIContainer.getRootView();
				_newView = (View) _model.getCurrentUIModuleView();
				// 设置headerView 的 宽高
				_newView.setLayoutParams(new LayoutParams((int) _model.getRealWidth(), (int) _model.getRealHeight()));
			} else {
				DoServiceContainer.getLogEngine().writeDebug("试图打开一个无效的页面文件:" + _uiPath);
			}
		}
		return _newView;
	}

	/**
	 * 动态修改属性值时会被调用，方法返回值为true表示赋值有效，并执行onPropertiesChanged，否则不进行赋值；
	 *
	 * @_changedValues<key,value>属性集（key名称、value值）；
	 */
	@Override
	public boolean onPropertiesChanging(Map<String, String> _changedValues) {
		return true;
	}

	/**
	 * 属性赋值成功后被调用，可以根据组件定义相关属性值修改UIView可视化操作；
	 *
	 * @_changedValues<key,value>属性集（key名称、value值）；
	 */
	@Override
	public void onPropertiesChanged(Map<String, String> _changedValues) {
		DoUIModuleHelper.handleBasicViewProperChanged(this.model, _changedValues);
		if (_changedValues.containsKey("vSpacing") || _changedValues.containsKey("vSpacing")) {
			int _hSpacing = DoTextHelper.strToInt(_changedValues.get("hSpacing"), 0);
			int _vSpacing = DoTextHelper.strToInt(_changedValues.get("vSpacing"), 0);
			WaterfallViewItemDecoration waterfallViewItemDecoration = new WaterfallViewItemDecoration((int) (_hSpacing * model.getXZoom()), (int) (_vSpacing * model.getYZoom()), columuCount);
			recyclerView.addItemDecoration(waterfallViewItemDecoration);
		}
		if (_changedValues.containsKey("templates")) {
			initViewTemplate(_changedValues.get("templates"));
		}

		if (_changedValues.containsKey("items")) {
			try {
				String _address = _changedValues.get("items");
				bindItems(_address);
			} catch (Exception _err) {
				DoServiceContainer.getLogEngine().writeError("do_WaterfallView_View items", _err);
			}
		}
		if (_changedValues.containsKey("numColumns")) {
			columuCount = DoTextHelper.strToInt(_changedValues.get("numColumns"), 1);
		}
	}

	/**
	 * 同步方法，JS脚本调用该组件对象方法时会被调用，可以根据_methodName调用相应的接口实现方法；
	 *
	 * @_methodName 方法名称
	 * @_dictParas 参数（K,V），获取参数值使用API提供DoJsonHelper类；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_invokeResult 用于返回方法结果对象
	 */
	@Override
	public boolean invokeSyncMethod(String _methodName, JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		if ("rebound".equals(_methodName)) {
			rebound(_dictParas, _scriptEngine, _invokeResult);
			return true;
		}
		if ("bindItems".equals(_methodName)) {
			bindItems(_dictParas, _scriptEngine, _invokeResult);
			return true;
		}
		if ("refreshItems".equals(_methodName)) {
			refreshItems(_dictParas, _scriptEngine, _invokeResult);
			return true;
		}
		if ("scrollToPosition".equals(_methodName)) {
			scrollToPosition(_dictParas, _scriptEngine, _invokeResult);
			return true;
		}
		return false;
	}

	/**
	 * 异步方法（通常都处理些耗时操作，避免UI线程阻塞），JS脚本调用该组件对象方法时会被调用， 可以根据_methodName调用相应的接口实现方法；
	 *
	 * @_methodName 方法名称
	 * @_dictParas 参数（K,V），获取参数值使用API提供DoJsonHelper类；
	 * @_scriptEngine 当前page JS上下文环境
	 * @_callbackFuncName 回调函数名 #如何执行异步方法回调？可以通过如下方法：
	 *                    _scriptEngine.callback(_callbackFuncName,
	 *                    _invokeResult);
	 *                    参数解释：@_callbackFuncName回调函数名，@_invokeResult传递回调函数参数对象；
	 *                    获取DoInvokeResult对象方式new
	 *                    DoInvokeResult(this.model.getUniqueKey());
	 */
	@Override
	public boolean invokeAsyncMethod(String _methodName, JSONObject _dictParas, DoIScriptEngine _scriptEngine, String _callbackFuncName) {
		// ...do something
		return false;
	}

	/**
	 * 释放资源处理，前端JS脚本调用closePage或执行removeui时会被调用；
	 */
	@Override
	public void onDispose() {
		// ...do something
	}

	/**
	 * 重绘组件，构造组件时由系统框架自动调用；
	 * 或者由前端JS脚本调用组件onRedraw方法时被调用（注：通常是需要动态改变组件（X、Y、Width、Height）属性时手动调用）
	 */
	@Override
	public void onRedraw() {
		this.setLayoutParams(DoUIModuleHelper.getLayoutParams(this.model));
	}

	/**
	 * 获取当前model实例
	 */
	@Override
	public DoUIModule getModel() {
		return model;
	}

	/**
	 * 绑定item的数据；
	 *
	 * @_dictParas 参数（K,V），可以通过此对象提供相关方法来获取参数值（Key：为参数名称）；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_invokeResult 用于返回方法结果对象
	 */
	@Override
	public void bindItems(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		String _address = DoJsonHelper.getString(_dictParas, "data", "");
		bindItems(_address, _scriptEngine);
	}

	private void bindItems(String _address, DoIScriptEngine _scriptEngine) throws Exception {
		if (_address == null || _address.length() <= 0)
			throw new Exception("do_WaterfallView 未指定相关的data参数！");
		DoMultitonModule _multitonModule = DoScriptEngineHelper.parseMultitonModule(_scriptEngine, _address);
		if (_multitonModule == null)
			throw new Exception("do_WaterfallView data参数无效！");
		if (_multitonModule instanceof DoIListData) {
			DoIListData _data = (DoIListData) _multitonModule;
			myAdapter.bindData(_data);
		}
		mySetAdapter();
	}

	private void bindItems(String _itmes) throws Exception {
		if (!TextUtils.isEmpty(_itmes)) {
			JSONArray _data = new JSONArray(_itmes);
			myAdapter.bindData(_data);
			mySetAdapter();
		}
	}

	private void mySetAdapter() {
		StaggeredGridLayoutManager staggeredGridLayoutManager = new StaggeredGridLayoutManager(columuCount, StaggeredGridLayoutManager.VERTICAL);
		staggeredGridLayoutManager.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_NONE);
		recyclerView.setLayoutManager(staggeredGridLayoutManager);
		recyclerView.setAdapter(myAdapter);
	}

	/**
	 * 复位；
	 *
	 * @_dictParas 参数（K,V），可以通过此对象提供相关方法来获取参数值（Key：为参数名称）；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_invokeResult 用于返回方法结果对象
	 */
	@Override
	public void rebound(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		if (mPullState == PULL_DOWN_STATE) {
			savaTime(System.currentTimeMillis());
			onHeaderRefreshComplete();
		}
	}

	/**
	 * 刷新item数据；
	 *
	 * @_dictParas 参数（K,V），可以通过此对象提供相关方法来获取参数值（Key：为参数名称）；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_invokeResult 用于返回方法结果对象
	 */
	@Override
	public void refreshItems(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		myAdapter.notifyDataSetChanged();
	}

	/**
	 * 平滑地滚动到特定位置；
	 *
	 * @_dictParas 参数（K,V），可以通过此对象提供相关方法来获取参数值（Key：为参数名称）；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_invokeResult 用于返回方法结果对象
	 */
	@Override
	public void scrollToPosition(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		int _position = DoJsonHelper.getInt(_dictParas, "position", 0);
		mIsSmooth = DoJsonHelper.getBoolean(_dictParas, "isSmooth", false);
		if (mIsSmooth) {
			recyclerView.smoothScrollToPosition(_position);
		} else {
			recyclerView.scrollToPosition(_position);
		}
	}

	private void initViewTemplate(String data) {
		try {
			myAdapter.initTemplates(data.split(","));
		} catch (Exception e) {
			DoServiceContainer.getLogEngine().writeError("解析cell属性错误： \t", e);
		}
	}

	// 设置事件监听
	private void initListener() {
		myAdapter.setOnItemListener(new WaterfallViewAdapter.OnItemListener() {
			@Override
			public void onTouch(View view, int position) {
				fireTouch(position);
			}

			@Override
			public void onTouch1(View view, int position, float x, float y) {
				fireTouch1(position, x, y);
			}

			@Override
			public void onLongTouch(View view, int position) {
				fireLongTouch(position);
			}

			@Override
			public void onLongTouch1(View view, int position, float x, float y) {
				fireLongTouch1(position, x, y);
			}
		});
		// scroll事件
		recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
			@Override
			public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
				// 当前状态为停止滑动状态SCROLL_STATE_IDLE时
				if (newState == RecyclerView.SCROLL_STATE_IDLE) {
//                    int lastPosition = -1;
//                    int firstPosition = 0;
//                    RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
//                    if (layoutManager instanceof StaggeredGridLayoutManager) {
//                        //因为StaggeredGridLayoutManager的特殊性可能导致最后显示的item存在多个，所以这里取到的是一个数组
//                        //得到这个数组后再取到数组中position值最大的那个就是最后显示的position值了
//                        int[] lastPositions = new int[((StaggeredGridLayoutManager) layoutManager).getSpanCount()];
//                        ((StaggeredGridLayoutManager) layoutManager).findLastVisibleItemPositions(lastPositions);
//                        lastPosition = findMax(lastPositions);
//
//                        int[] firstPositions = new int[((StaggeredGridLayoutManager) layoutManager).getSpanCount()];
//                        ((StaggeredGridLayoutManager) layoutManager).findFirstVisibleItemPositions(firstPositions);
//                        firstPosition = findMin(firstPositions);
//                        fireScroll(firstPosition, lastPosition);
//                    }
				}
			}

			private int overallYScroll = 0;

			@Override
			public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
				super.onScrolled(recyclerView, dx, dy);
				overallYScroll = overallYScroll + dy;// 根据每次移动的值 求出scrollY
				if (overallYScroll <= 0)
					isTop = true;
				else
					isTop = false;
			}
		});
	}

	private void fireTouch(int position) {
		DoInvokeResult _invokeResult = new DoInvokeResult(this.model.getUniqueKey());
		_invokeResult.setResultInteger(position);
		this.model.getEventCenter().fireEvent("touch", _invokeResult);
	}

	private void fireTouch1(int position, float x, float y) {
		DoInvokeResult _invokeResult = new DoInvokeResult(this.model.getUniqueKey());
		JSONObject _obj = new JSONObject();
		try {
			_obj.put("position", position);
			_obj.put("x", x / this.model.getXZoom());
			_obj.put("y", y / this.model.getYZoom());
		} catch (Exception e) {
		}
		_invokeResult.setResultNode(_obj);
		this.model.getEventCenter().fireEvent("touch1", _invokeResult);
	}

	private void fireLongTouch(int position) {
		DoInvokeResult _invokeResult = new DoInvokeResult(this.model.getUniqueKey());
		_invokeResult.setResultInteger(position);
		this.model.getEventCenter().fireEvent("longTouch", _invokeResult);
	}

	private void fireLongTouch1(int position, float x, float y) {
		DoInvokeResult _invokeResult = new DoInvokeResult(this.model.getUniqueKey());
		JSONObject _obj = new JSONObject();
		try {
			_obj.put("position", position);
			_obj.put("x", x / this.model.getXZoom());
			_obj.put("y", y / this.model.getYZoom());
		} catch (Exception e) {
		}
		_invokeResult.setResultNode(_obj);
		this.model.getEventCenter().fireEvent("longTouch1", _invokeResult);
	}

	@Override
	protected void fireEvent(int mHeaderState, int newTopMargin, String eventName) {
		int offset = mHeaderView.getHeight() + newTopMargin;
		if (mHeaderState == RELEASE_TO_REFRESH) {
			offset = mHeaderView.getHeight();
		}
		DoInvokeResult _invokeResult = new DoInvokeResult(this.model.getUniqueKey());
		try {
			JSONObject _node = new JSONObject();
			_node.put("state", mHeaderState);
			_node.put("offset", (Math.abs(offset) / this.model.getYZoom()) + "");
			_invokeResult.setResultNode(_node);
			this.model.getEventCenter().fireEvent(eventName, _invokeResult);
		} catch (Exception _err) {
			DoServiceContainer.getLogEngine().writeError("do_WaterfallView " + eventName + " \n", _err);
		}
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		mAdapterView = this.recyclerView;
	}

	// 用于实现行间距和列间距
	public class WaterfallViewItemDecoration extends ItemDecoration {
		private int _hSpace = 0;
		private int _vSpace = 0;
		private int _columnCount;

		public WaterfallViewItemDecoration(int hSpace, int vSpace, int columnCount) {
			this._hSpace = hSpace;
			this._vSpace = vSpace;
			this._columnCount = columnCount;
		}

		@Override
		public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
			outRect.bottom = _vSpace;
			StaggeredGridLayoutManager.LayoutParams lp = (StaggeredGridLayoutManager.LayoutParams) view.getLayoutParams();
			// 获取该view位于第几列
			int spanIndex = lp.getSpanIndex();
			if (spanIndex == 0) {
				// 左边一列
				outRect.left = _hSpace;
				outRect.right = _hSpace / 2;

			} else if (spanIndex == _columnCount - 1) {
				// 右边一列
				outRect.left = _hSpace / 2;
				outRect.right = _hSpace;
			} else {
				// 中间列
				outRect.left = _hSpace / 2;
				outRect.right = _hSpace / 2;
			}
		}
	}
}
package doext.implement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.support.v7.widget.RecyclerView.Adapter;
import android.util.SparseIntArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.FrameLayout;
import core.DoServiceContainer;
import core.helper.DoJsonHelper;
import core.helper.DoTextHelper;
import core.helper.DoUIModuleHelper;
import core.interfaces.DoIListData;
import core.interfaces.DoIUIModuleView;
import core.object.DoSourceFile;
import core.object.DoUIContainer;
import core.object.DoUIModule;


/**
 * Created by Administrator on 2017/11/28.
 */

public class WaterfallViewAdapter extends Adapter<WaterfallViewHolder> {
    private Map<String, String> viewTemplates = new HashMap<String, String>();
    private List<String> cellTemplates = new ArrayList<String>();
    private SparseIntArray datasPositionMap = new SparseIntArray();
    private Object data;
    private DoUIModule currentUIModule;
    private OnItemListener myItemListener;

    public WaterfallViewAdapter(DoUIModule doUIModule) {
        currentUIModule = doUIModule;
    }

    public void setOnItemListener(OnItemListener onItemClickedListener) {
        this.myItemListener = onItemClickedListener;
    }

    public void bindData(DoIListData _listData) {
        this.data = _listData;
        myNotifyDataSetChanged();
    }

    public void bindData(JSONArray _array) {
        this.data = _array;
        myNotifyDataSetChanged();
    }

    private void myNotifyDataSetChanged() {
        int _size = getCount();
        for (int i = 0; i < _size; i++) {
            try {
                JSONObject childData = (JSONObject) getItem(i);
                Integer _index = DoTextHelper.strToInt(DoJsonHelper.getString(childData, "template", "0"), 0);
                if (_index >= cellTemplates.size() || _index < 0) {
                    DoServiceContainer.getLogEngine().writeError("索引不存在", new Exception("索引 " + _index + " 不存在"));
                    _index = 0;
                }
                datasPositionMap.put(i, _index);
            } catch (Exception e) {
                DoServiceContainer.getLogEngine().writeError("解析data数据错误： \t", e);
            }
        }
        notifyDataSetChanged();
    }

    public int getCount() {
        if (data == null) {
            return 0;
        }
        if (data instanceof DoIListData) {
            return ((DoIListData) data).getCount();
        }
        return ((JSONArray) data).length();
    }

    @Override
    public WaterfallViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = DoServiceContainer.getPageViewFactory().getAppContext();
        FrameLayout view = new FrameLayout(context);
        WaterfallViewHolder myViewHolder = new WaterfallViewHolder(view, myItemListener);
        return myViewHolder;
    }
    
    

    @Override
    public void onBindViewHolder(WaterfallViewHolder holder, int position) {
        try {
            JSONObject childData = (JSONObject) getItem(position);
            DoIUIModuleView _doIUIModuleView = null;
            int _index = DoTextHelper.strToInt(DoJsonHelper.getString(childData, "template", "0"), 0);
            if (_index >= cellTemplates.size() || _index < 0) {
                DoServiceContainer.getLogEngine().writeError("索引不存在", new Exception("索引 " + _index + " 不存在"));
                _index = 0;
            }
            String templateUI = cellTemplates.get(_index);

            String content = viewTemplates.get(templateUI);
            DoUIContainer _doUIContainer = new DoUIContainer(currentUIModule.getCurrentPage());
            _doUIContainer.loadFromContent(content, null, null);
            _doUIContainer.loadDefalutScriptFile(templateUI);// @zhuozy效率问题，listview第一屏可能要加载多次模版、脚本，需改进需求设计；
            _doIUIModuleView = _doUIContainer.getRootView().getCurrentUIModuleView();

            if (_doIUIModuleView != null) {
                _doIUIModuleView.getModel().setModelData(childData);
                FrameLayout frameLayout = (FrameLayout) holder.itemView;
                View view = (View) _doIUIModuleView;
                view.setLayoutParams(new AbsListView.LayoutParams(_columnWidth, (int) _doIUIModuleView.getModel().getRealHeight()));
                frameLayout.addView(view);
                if (holder.itemView instanceof ViewGroup) {
                    ((ViewGroup) holder.itemView).setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
                }
            }
        } catch (Exception e) {
            DoServiceContainer.getLogEngine().writeError("解析data数据错误： \t", e);
        }
    }

    @Override
    public int getItemCount() {
        if (data == null) {
            return 0;
        }
        if (data instanceof DoIListData) {
            return ((DoIListData) data).getCount();
        }
        return ((JSONArray) data).length();
    }

    @Override
    public int getItemViewType(int position) {
        return datasPositionMap.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }



    public Object getItem(int position) {
        try {
            if (data instanceof DoIListData) {
                return ((DoIListData) data).getData(position);
            }
            return ((JSONArray) data).getJSONObject(position);
        } catch (JSONException e) {
            DoServiceContainer.getLogEngine().writeError("do_GridView_View getItem \n\t", e);
        }
        return position;
    }

    int _columnWidth = 0;

    public void initTemplates(String[] templates) throws Exception {
        cellTemplates.clear();
        for (String templateUi : templates) {
            if (templateUi != null && !templateUi.equals("")) {
                DoSourceFile _sourceFile = currentUIModule.getCurrentPage().getCurrentApp().getSourceFS().getSourceByFileName(templateUi);
                if (_sourceFile != null) {
                    viewTemplates.put(templateUi, _sourceFile.getTxtContent());
                    cellTemplates.add(templateUi);
                } else {
                    throw new Exception("试图使用一个无效的页面文件:" + templateUi);
                }
            }
        }
        //取出第一个cell，计算cell 的宽
        if (viewTemplates != null && viewTemplates.size() > 0) {
            _columnWidth = getCellRealWidth(viewTemplates.get(cellTemplates.get(0)));
        }
    }

    public int getCellRealWidth(String Context) throws Exception {
        JSONObject _pageRootNode = DoJsonHelper.loadDataFromText(Context).getJSONObject("RootView");
        if (_pageRootNode != null) {
            int _width = DoTextHelper.strToInt(DoJsonHelper.getString(_pageRootNode, "width", "-1"), -1);
            return DoUIModuleHelper.getCalcValue(currentUIModule.getXZoom() * _width);
        }
        return -1;
    }

    /**
     * 点击回调的接口
     */
    public interface OnItemListener {
        void onTouch(View view, int position);

        void onTouch1(View view, int position, float x, float y);

        void onLongTouch(View view, int position);

        void onLongTouch1(View view, int position, float x, float y);
    }

}
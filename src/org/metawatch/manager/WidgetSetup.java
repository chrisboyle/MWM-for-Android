package org.metawatch.manager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.metawatch.manager.MetaWatchService.Preferences;
import org.metawatch.manager.widgets.InternalWidget.WidgetData;
import org.metawatch.manager.widgets.WidgetManager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SimpleExpandableListAdapter;

public class WidgetSetup extends Activity {
	
	private ExpandableListView widgetList;
	private SimpleExpandableListAdapter adapter;
	
	private List<Map<String, String>> groupData;
	private List<List<Map<String, String>>> childData;
	
	private Map<String,WidgetData> widgetMap;
	
    private static final String NAME = "NAME";
    private static final String ID = "ID";
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.widget_setup);  
    }
    
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        
        setContentView(R.layout.widget_setup);
        adapter = null;
        onStart();
    }
    
	@Override
	protected void onStart() {
		super.onStart();
		
		if(adapter!=null)
			return;
		
		widgetMap = WidgetManager.getCachedWidgets(this, null);
			
		widgetList = (ExpandableListView) findViewById(R.id.widgetList);		
		widgetList.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
			
			public boolean onChildClick(ExpandableListView parent, View v,
					int groupPosition, int childPosition, long id) {
				Intent i = new Intent(getApplicationContext(), WidgetPicker.class);
				i.putExtra("groupPosition", groupPosition);
				i.putExtra("childPosition", childPosition);
				startActivityForResult(i,  1);
				return false;
			}
		});
				
		groupData = new ArrayList<Map<String, String>>();
	    childData = new ArrayList<List<Map<String, String>>>();

	    // Add dummy entries at the end
		ArrayList<String> rows = new ArrayList<String>(Arrays.asList(MetaWatchService.getWidgets(this).split("\\|")));
		if (rows.size()>0 && rows.get(rows.size()-1).length()>0) {
			rows.add("");
		}
		while(rows.size()<9)
			rows.add("");
			
		int i=1;
		for(String line : rows) {

			String[] widgets = (line).split(",");
			
			Map<String, String> curGroupMap = new HashMap<String, String>();
	        groupData.add(curGroupMap);
	        
	        List<Map<String, String>> children = new ArrayList<Map<String, String>>();
	        
	        int widgetCount = 0;
	        
			for(String widget : widgets) {
				widget = widget.trim();
	        	Map<String, String> curChildMap = new HashMap<String, String>();
	            children.add(curChildMap);
	            String name = widget;
	            if(widget==null || widget=="")
	            	name="<empty>";
	            if(widgetMap.containsKey(widget)) {
	            	name = widgetMap.get(widget).description;
	            	widgetCount++;
	            }
	            curChildMap.put(NAME, name);
	            curChildMap.put(ID, widget);
	        }
			
			while(children.size()<8) {
	        	Map<String, String> curChildMap = new HashMap<String, String>();
	            children.add(curChildMap);
	            curChildMap.put(NAME, "<empty>");
	            curChildMap.put(ID, "");
			}
			
			
	        
	        curGroupMap.put(NAME, getGroupName(i++, widgetCount));
	        curGroupMap.put(ID, "Id");
			
	        childData.add(children);
		}	    
	            
	    // Set up our adapter
		adapter = new SimpleExpandableListAdapter(
			this,
			groupData,
			android.R.layout.simple_expandable_list_item_1,
			new String[] { NAME, ID },
			new int[] { android.R.id.text1, android.R.id.text2 },
			childData,
			android.R.layout.simple_expandable_list_item_2,
			new String[] { NAME, ID },
			new int[] { android.R.id.text1, android.R.id.text2 }
		);
	    widgetList.setAdapter(adapter);
		
		refreshPreview();
	}
	
	private String getGroupName(int group, int widgetCount) {
        StringBuilder nameSb = new StringBuilder();
        nameSb.append("Row ");
        nameSb.append(group);
        nameSb.append(" ");

        if(widgetCount==0) {
        	nameSb.append("(empty)");
        }
        else if(widgetCount==1) {
        	nameSb.append("(1 widget)");
        }
        else {
        	nameSb.append("(");
        	nameSb.append(widgetCount);
        	nameSb.append(" widgets)");
        }
        
        return nameSb.toString();
	}
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);

		widgetMap = WidgetManager.getCachedWidgets(this, null);
    	
        if (resultCode == Activity.RESULT_OK) {      	  
        	String id = data.getStringExtra("selectedWidget");
        	int groupPosition = data.getIntExtra("groupPosition", -1);
        	int childPosition = data.getIntExtra("childPosition", -1);
        	
        	if(groupPosition>-1 && childPosition>-1) {
        		Map<String,String> curChildMap = childData.get(groupPosition).get(childPosition);
        		if(id==null || id.equalsIgnoreCase("")) {
    	            curChildMap.put(NAME, "<empty>");
    	            curChildMap.put(ID, "");
        		}
        		else {
    	            String name = id;
    	            if(widgetMap.containsKey(id))
    	            	name = widgetMap.get(id).description;
    	            curChildMap.put(NAME, name);
		            curChildMap.put(ID, id);
        		}
        	}
        	
        	int widgetCount = 0;
        	int childCount = childData.get(groupPosition).size();
        	for(int i=0; i<childCount;++i) {
        		Map<String,String> curChildMap = childData.get(groupPosition).get(i);
        		if(!curChildMap.get(ID).equalsIgnoreCase("")) {
        			widgetCount++;
        		}
        	}
        	
        	groupData.get(groupPosition).put(NAME, getGroupName(groupPosition+1, widgetCount));
        	
        	adapter.notifyDataSetChanged();
        	storeWidgetLayout();
        	refreshPreview();
        	Idle.updateIdle(this, true);
	        if(MetaWatchService.watchType == MetaWatchService.WatchType.ANALOG) {
	        	Idle.sendOledIdle(this);
	        }
        }
    }
    
    private void refreshPreview() {
    	Idle.updateIdlePages(this, true);
    	LinearLayout ll = (LinearLayout) findViewById(R.id.idlePreviews);
    	
    	ll.removeAllViews();
    	  	
    	int pages = Idle.numPages();
    	for(int i=0; i<pages; ++i) {
    		Bitmap bmp = Idle.createIdle(this, true, i);;

    		if (bmp!=null) {
    			
    			int backCol = Color.LTGRAY;
    			int viewId = (MetaWatchService.watchType == MetaWatchService.WatchType.ANALOG) 
    					? R.layout.idle_screen_preview_oled
    				    : R.layout.idle_screen_preview;
    			
        		if(Preferences.invertLCD || MetaWatchService.watchType == MetaWatchService.WatchType.ANALOG) {
        			Utils.invertBitmap(bmp);
        			backCol = 0xff111111;
        		}
        		    			
	    		LayoutInflater factory = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	
	    		View v = factory.inflate(viewId, null);
	    		ImageView iv = (ImageView)v.findViewById(R.id.image);
	    		iv.setImageBitmap(bmp);
	    		iv.setClickable(true);
	    		iv.setBackgroundColor(backCol);
	    		iv.setTag(i);
	    		iv.setOnClickListener(new OnClickListener() {
	    		    //@Override
	    		    public void onClick(View v) {
	    		    	Integer page = (Integer)v.getTag();
	    		        Idle.toPage(page);
	    		        Idle.updateIdle(v.getContext(), true);
	    		        
	    		        if(MetaWatchService.watchType == MetaWatchService.WatchType.ANALOG) {
	    		        	Idle.sendOledIdle(v.getContext());
	    		        }
	    		    }
	    		});
	    		ll.addView(v);
    		}
    	}
    }
    
    private void storeWidgetLayout() {
    	
        StringBuilder out = new StringBuilder();
    	for(List<Map<String, String>> row : childData) {
    		if(out.length()>0)
    			out.append("|");
    		
    		StringBuilder line = new StringBuilder();
    		for(Map<String, String> child : row) {
        		String id = child.get(ID);	
        		if(id!="") {
	        		if(line.length()>0)
	        			line.append(",");
	        		
	        		line.append(id);
        		}
    		}
    		
    		out.append(line);
    	}
    	
    	MetaWatchService.saveWidgets(this, out.toString());
    }
}

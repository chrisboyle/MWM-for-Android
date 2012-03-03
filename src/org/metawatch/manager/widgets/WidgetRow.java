package org.metawatch.manager.widgets;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.metawatch.manager.widgets.InternalWidget.WidgetData;

import android.graphics.Canvas;

public class WidgetRow {
	
	List<String> widgetIDs = new ArrayList<String>();
	
	public void add(String id) {
		widgetIDs.add(id);
	}
	
	public List<String> getIds() {
		return widgetIDs;
	}
	
	public int draw(Map<String,WidgetData> widgetData, Canvas canvas, int y)
	{
		List<WidgetData> widgets = new ArrayList<WidgetData>();
				
		int totalWidth = 0, maxHeight = 0;
		for( String id : widgetIDs ) {
			WidgetData widget = widgetData.get(id);
			if(widget!=null && widget.bitmap!=null && widget.priority>-1) {
				widgets.add(widget);
				totalWidth += widget.width;
				if (widget.height > maxHeight) maxHeight = widget.height;
			}
		}
		
		// Cull widgets to fit

		while(totalWidth>96) {
			int lowestPri = Integer.MAX_VALUE;
			int cull = -1;
			for(int i=0; i<widgets.size(); ++i) {
				int pri = widgets.get(i).priority;
				if(pri <= lowestPri) {
					cull = i;
					lowestPri = pri;
				}
			}
			if(cull>-1)
			{
				totalWidth-=widgets.get(cull).width;
				widgets.remove(cull);
			}
			else
			{
				return 0;
			}
		}
		
		int space = (96-totalWidth)/(widgets.size()+1);		
		int x=space;
		for(WidgetData widget : widgets) {
			canvas.drawBitmap(widget.bitmap, x,
					y + maxHeight/2 - widget.height/2, null);
			x += (space+widget.width);
		}

		return maxHeight;
	}
}

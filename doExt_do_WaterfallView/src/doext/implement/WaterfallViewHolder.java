package doext.implement;

import doext.implement.WaterfallViewAdapter.OnItemListener;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.View;

public class WaterfallViewHolder extends ViewHolder {

	public WaterfallViewHolder(View itemView, final OnItemListener myItemListener) {
		super(itemView);
		itemView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				myItemListener.onTouch(v, getAdapterPosition());
				myItemListener.onTouch1(v, getAdapterPosition(), v.getX(), v.getY());
			}
		});
		itemView.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				myItemListener.onLongTouch(v, getAdapterPosition());
				myItemListener.onLongTouch1(v, getAdapterPosition(), v.getX(), v.getY());
				return true;
			}
		});
	}
}

package org.sid.bluetoothsearch;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.sid.bluetoothsearch.datarepository.Bluetooth;
import java.util.List;

public class BlAdapter extends RecyclerView.Adapter<BlAdapter.ViewHolder>{
    private final Context mContext;
    private final LayoutInflater mInflater;
    private List<Bluetooth> mDataset = null;
    private PopupWindow mPopup;
    private LinearLayout parentLayout;
    private final OnItemClickListener mListener;
    private boolean popupIsDismissed = true;
    public boolean isDarkTheme = false;

    public BlAdapter(Context context, OnItemClickListener listener, LinearLayout Layout) {

        this.mContext = context;
        this.mListener = listener;
        mInflater = LayoutInflater.from(context);
        this.parentLayout = Layout;
    }

    //On cree une nouvelle vue
    public BlAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.item_recycler, parent, false);
        return new ViewHolder(view);
    }

    // On remplace le contenu de la vue@Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // On recupere l'element de dataset a cette position
        // On remplace le contenu
        holder.bind(mDataset.get(position));
    }

    // ROn retourne la taille du dataset
    public int getItemCount() {
        return (mDataset != null) ? mDataset.size() : 0;
    }

    public void swapDataset(List<Bluetooth> dataset) {
        // on echange les donnees
        this.mDataset = dataset;
        notifyDataSetChanged();
    }

    public interface OnItemClickListener {
        void onFavoriteIconClicked(Bluetooth device, ImageView icon);
        void onShareIconClicked(Bluetooth device);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView shareIcon;
        private TextView textView;
        private ImageView favIcon;
        private View parent;

        ViewHolder(@NonNull View view) {
            super(view);
            parent = view;
            textView = view.findViewById(R.id.device_tv);
            favIcon = view.findViewById(R.id.icon_favorite_btn);
            shareIcon = view.findViewById(R.id.shareme);
        }

        void bind(Bluetooth device) {
            String name = device.getbName();
            String address = device.getbAddress();
            String type = device.getbType();
            String bonded = device.getbBounded();
            String majorClass = device.getMajorClass();
            bindViews(String.format("Name: %s\nMAC: %s\n----------\n",
                    name, address), parent, textView);

            if (device.isFavorited) {
                favIcon.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_favorite));
            } else {
                favIcon.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_unfavorite));
            }

            favIcon.setOnClickListener(v -> {
                device.isFavorited = !device.isFavorited;
                mListener.onFavoriteIconClicked(device, favIcon);
            });

            shareIcon.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_share));
            shareIcon.setOnClickListener(v -> {
                mListener.onShareIconClicked(device);
            });


            /**
             * We set onclick listener to display more information on the bluetooth device.
             * The information is displayed on a popup window
             */
            textView.setOnClickListener(v -> {
                if (popupIsDismissed) {
                    popupIsDismissed = false;
                    //LogWrapper.d("textView Clicked");
                    LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    View popupView = inflater.inflate(R.layout.pop_info, null);

                    mPopup = new PopupWindow(
                            popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT

                    );
                    if (Build.VERSION.SDK_INT >= 21) {
                        mPopup.setElevation(5.0f);
                    }

                    TextView textV = popupView.findViewById(R.id.popupInfoBlue);
                    bindViews(String.format("Name: %s\nMAC: %s\n%s\n%s\n%s\n----------\n",
                            name, address, type, bonded, majorClass), parent, textV);


                    ImageView closebtn = popupView.findViewById(R.id.btn_closePop);
                    closebtn.setOnClickListener(view -> {
                        mPopup.dismiss();
                        popupIsDismissed = true;
                    });

                    mPopup.showAtLocation(parentLayout, Gravity.CENTER, 0, 0);

                }
            });
        }

        private void bindViews(String text, View view, TextView textOfView) {
            if (isDarkTheme) {
                textOfView.setTextColor(Color.WHITE);
                view.setBackgroundColor(Color.rgb(73, 82, 89));
            } else {
                textOfView.setTextColor(Color.BLACK);
                view.setBackgroundColor(Color.rgb(223, 221, 226));
            }
            textOfView.setText(text);

        }
    }
}

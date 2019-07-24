package com.github.sunnyseaside.timezonefixer;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.design.widget.FloatingActionButton;
import android.view.*;
import android.widget.EditText;
import android.widget.TextView;

import java.util.Date;

/**
 * An activity representing a list of Items. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link ItemDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */
public class ItemListActivity extends AppCompatActivity {

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;
    private PhotoDataManager manager;

    private final static int REQUEST_CODE_READ=1;

    private Thread fixall_thread;
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_item_list);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(getTitle());

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);

        final Context context=this;
        final Handler handler=new Handler(Looper.getMainLooper());
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final ProgressDialog dialog=new ProgressDialog(context);///todo deprecated
                dialog.setTitle("Please wait");
                dialog.setMessage("Fixing all images...");
                dialog.setIndeterminate(false);
                dialog.setMax(manager.getCount());
                dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                dialog.show();
                /*Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();*/
                //TODO use AsyncTask?
                fixall_thread=new Thread(){
                    @Override public void run(){
                        manager.fixAll(handler,new PhotoDataManager.ProgressListener(){
                            @Override public void onProgress(int progress){
                                dialog.setProgress(progress);
                            }
                            @Override public void onFinished(){
                                dialog.dismiss();
                            }
                        });
                    }
                };
                fixall_thread.start();
                //fixall_thread.run();
            }
        });

        //ActionBar

        if (findViewById(R.id.item_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-w900dp).
            // If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;
        }

        //final PhotoDataManager viewModel=ViewModelProviders.of(this).get(PhotoDataManager.class);
        //viewModel.startQuery();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            doRead();
        } else {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},REQUEST_CODE_READ);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch(requestCode){
            case REQUEST_CODE_READ:
                if(grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    doRead();
                } else finish();
        }
    }

    private void doRead(){
        manager=new PhotoDataManager(this);

        recyclerView = findViewById(R.id.item_list);
        assert recyclerView != null;
        setupRecyclerView((RecyclerView) recyclerView);
    }
    @Override public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.main,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        AlertDialog.Builder builder;
        switch(item.getItemId()){
            case R.id.menu_filter_order:
                LayoutInflater inflater=getLayoutInflater();
                View view=inflater.inflate(R.layout.dialog_query,null);
                builder=new AlertDialog.Builder(this);
                builder.setView(view);//R.layout.dialog_query);
                final EditText txtFilter=view.findViewById(R.id.txtFilter);
                final EditText txtOrder=view.findViewById(R.id.txtOrder);

                builder.setTitle(R.string.dlg_query);
                builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        manager.requery(txtFilter.getText().toString(),null,txtOrder.getText().toString());
                        recyclerView.getAdapter().notifyDataSetChanged();
                    }
                });
                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {}
                });
                builder.show();
                return true;
            case R.id.menu_about:
                builder=new AlertDialog.Builder(this);
                builder.setTitle(R.string.about_title);
                builder.setMessage(R.string.about_text);
                builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {}
                });
                builder.show();
                return true;
        }
        return false;
    }

    private void setupRecyclerView(@NonNull RecyclerView recyclerView) {
        recyclerView.setAdapter(new PhotoItemViewAdapter(this, manager, mTwoPane));
    }

    public static class PhotoItemViewAdapter
            extends RecyclerView.Adapter<PhotoItemViewAdapter.ViewHolder> {

        private final ItemListActivity mParentActivity;
        private final PhotoDataManager manager;
        private final boolean mTwoPane;
        private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ///TODO implement
                int pos= (Integer) view.getTag();
                PhotoDataManager.PhotoData data=manager.get(pos);
                data.doFix();
                /*if (mTwoPane) {
                    Bundle arguments = new Bundle();
                    arguments.putString(ItemDetailFragment.ARG_ITEM_ID, item.id);
                    ItemDetailFragment fragment = new ItemDetailFragment();
                    fragment.setArguments(arguments);
                    mParentActivity.getSupportFragmentManager().beginTransaction()
                            .replace(R.id.item_detail_container, fragment)
                            .commit();
                } else {
                    Context context = view.getContext();
                    Intent intent = new Intent(context, ItemDetailActivity.class);
                    intent.putExtra(ItemDetailFragment.ARG_ITEM_ID, item.id);

                    context.startActivity(intent);
                }*/
            }
        };

        PhotoItemViewAdapter(ItemListActivity parent,
                             PhotoDataManager m,
                             boolean twoPane) {
            manager=m;
            mParentActivity = parent;
            mTwoPane = twoPane;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_list_content, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            PhotoDataManager.PhotoData pd=manager.get(position);

            Date date=new Date(pd.getDateTaken());
            holder.mIdView.setText(pd.getName());
            holder.mContentView.setText((pd.shouldFix()?"Y":"N")+date.toString());

            holder.itemView.setTag(position);
            holder.itemView.setOnClickListener(mOnClickListener);
        }

        @Override
        public int getItemCount() {
            return manager.getCount();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final TextView mIdView;
            final TextView mContentView;

            ViewHolder(View view) {
                super(view);
                mIdView = (TextView) view.findViewById(R.id.id_text);
                mContentView = (TextView) view.findViewById(R.id.content);
            }
        }
    }
}

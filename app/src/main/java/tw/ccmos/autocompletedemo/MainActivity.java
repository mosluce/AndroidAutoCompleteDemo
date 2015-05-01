package tw.ccmos.autocompletedemo;

import android.content.Context;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.GsonBuilder;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.BaseJsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.Header;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import bolts.Continuation;
import bolts.Task;
import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnTextChanged;


public class MainActivity extends ActionBarActivity {

    @InjectView(R.id.queryField)
    AutoCompleteTextView queryField;

    AssetAdapter assetAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.inject(this);

        assetAdapter = new AssetAdapter(this, R.layout.simple_list_item, R.id.label);
        queryField.setAdapter(assetAdapter);
        queryField.setThreshold(1);


    }

    @OnTextChanged(R.id.queryField)
    void onTextChanged(CharSequence text) {
        if (!text.toString().isEmpty()) {
            query(text.toString())
                    .onSuccess(new Continuation<AssetResult, Object>() {
                        @Override
                        public Object then(Task<AssetResult> task) throws Exception {
                            AssetResult result = task.getResult();
                            assetAdapter.clear();
                            assetAdapter.addAll(result.data);
                            assetAdapter.notifyDataSetChanged();
                            return null;
                        }
                    });
        }
    }

    private Task<AssetResult> query(String s) {
        final Task<AssetResult>.TaskCompletionSource tcs = Task.create();
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        params.put("keywords", s);
        client.post("http://keyleadcloudapi.azurewebsites.net/asset/assets/query", params, new BaseJsonHttpResponseHandler<AssetResult>() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, String rawJsonResponse, AssetResult response) {
                tcs.setResult(response);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, String rawJsonData, AssetResult errorResponse) {
                tcs.setError(new Exception(throwable));
            }

            @Override
            protected AssetResult parseResponse(String rawJsonData, boolean isFailure) throws Throwable {
                return new GsonBuilder().create().fromJson(rawJsonData, AssetResult.class);
            }
        });
        return tcs.getTask();
    }

    public class AssetAdapter extends ArrayAdapter<Asset> implements Filterable {

        @InjectView(R.id.label)
        TextView label;

        List<Asset> items;

        public AssetAdapter(Context context, int resource, int textViewResourceId) {
            super(context, resource, textViewResourceId);

            items = new ArrayList<>();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);

            ButterKnife.inject(this, view);
            label.setText(getItem(position).resource.name);

            return view;
        }

        @Override
        public void add(Asset object) {
            super.add(object);

            items.add(object);
        }

        @Override
        public void addAll(Collection<? extends Asset> collection) {
            super.addAll(collection);

            items.addAll(collection);
        }

        @Override
        public void clear() {
            super.clear();

            items.clear();
        }

        @Override
        public Filter getFilter() {
            Filter filter = new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence charSequence) {
                    FilterResults filterResults = new FilterResults();
                    filterResults.values = items;
                    filterResults.count = getCount();
                    return filterResults;
                }

                @Override
                protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                    notifyDataSetChanged();
                }
            };

            return filter;
        }
    }

    public class Result {
        public boolean success;
        public String message;
    }

    public class Asset {
        public Resource resource;
        public Category category;
    }

    private class Category {
        public String name;
        public String code;
    }

    public class Resource {
        public String name;
        public String code;
    }

    public class AssetResult extends Result {
        public List<Asset> data;
    }


}

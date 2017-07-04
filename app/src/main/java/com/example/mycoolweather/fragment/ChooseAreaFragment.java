package com.example.mycoolweather.fragment;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mycoolweather.MainActivity;
import com.example.mycoolweather.R;
import com.example.mycoolweather.activity.WeatherActivity;
import com.example.mycoolweather.db.City;
import com.example.mycoolweather.db.County;
import com.example.mycoolweather.db.Province;
import com.example.mycoolweather.util.HttpUtil;
import com.example.mycoolweather.util.Utility;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by Administrator on 2017/7/3.     ctrl + shift +u 大小写切换
 */

public class ChooseAreaFragment extends Fragment {
    public static final int LEVEL_PROVINCE =0;
    public static final int LEVEL_CITY = 1;
    public static final int LEVEL_COUNTY =2;

    private ProgressDialog progressDialog;

    private TextView titleText;
    private Button backButton;
    private ListView listView;

    private ArrayAdapter<String> adapter;
    private List<String> dataList = new ArrayList<>();

    /**
     * 省列表
     */
    private  List<Province> provinceList;
    /**
     * 市列表
     */
    private  List<City>cityList;
    /**
     *
     * 县列表
     *
     */
    private  List<County> countyList;
    /**
     * 选中的省份
     */
    private  Province selectedProvince;
    /**
     * 选中的城市
     */
    private  City selectedCity;

    /**
     * 当前选中的级别
     */
    private  int currentLevel;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view =inflater.inflate(R.layout.choose_area,container,false);
        titleText = (TextView) view.findViewById(R.id.title_text);
        backButton = (Button) view.findViewById(R.id.back_button);
        listView = (ListView) view.findViewById(R.id.list_view);

        adapter = new ArrayAdapter<String>(getContext(),android.R.layout.simple_list_item_1,dataList);
        listView.setAdapter(adapter);
        return view;

    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                   if(currentLevel == LEVEL_PROVINCE){
                       selectedProvince =provinceList.get(position);
                       queryCities();
                   }  else if(currentLevel == LEVEL_CITY){
                       selectedCity =cityList.get(position);
                       queryCounties();
                   }else if (currentLevel == LEVEL_COUNTY){
                       String weatherId = countyList.get(position).getWeatherId();

                       if(getActivity() instanceof MainActivity){

                           Intent intent = new Intent(getActivity(), WeatherActivity.class);
                           intent.putExtra("weather_id",weatherId);
                           startActivity(intent);
                           getActivity().finish();

                       }else if(getActivity() instanceof  WeatherActivity){
                           WeatherActivity weatherActivity = (WeatherActivity) getActivity();
                           weatherActivity.drawerLayout.closeDrawers();
                           weatherActivity.swipeRefresh.setRefreshing(true);
                           weatherActivity.requestWeather(weatherId);
                       }


                   }
            }
        });
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                 if(currentLevel == LEVEL_COUNTY){
                     queryCities();
                 }  else if (currentLevel == LEVEL_CITY){
                     queryProvinces();
                 }
            }
        });

        queryProvinces();
    }

    /**
     * 查询全国所有的省
     */
    private void queryProvinces() {
        titleText.setText("中国");
        backButton.setVisibility(View.GONE);

        provinceList = DataSupport.findAll(Province.class);
        if(provinceList.size() >0){
            dataList.clear();
            for (Province province:provinceList){
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel =LEVEL_PROVINCE;
        }else {
            String  address ="http://guolin.tech/api/china";
            queryFromServer(address,"province");
        }
    }

    /**
     *     根据传入的地址和类型从服务器上查询省市县数据
      * @param address
     * @param type
     */
    private void queryFromServer(String address, final String type) {
        showProggressDialog();

        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                       getActivity().runOnUiThread(new Runnable() {
                           @Override
                           public void run() {
                               closeProgressDialog();
                               Toast.makeText(getContext(),"加载失败。。。",Toast.LENGTH_SHORT).show();
                           }
                       });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                   String responseText = response.body().string();
                boolean result =false;
                if("province".equals(type)){
                    result = Utility.handleProvinceResponse(responseText);
                } else if("city".equals(type)){
                    result =Utility.handleCityResponse(responseText,selectedProvince.getId());
                } else if("county".equals(type)){
                    result =Utility.handleCountyResponse(responseText,selectedCity.getId());
                }

                if(result){
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if("province".equals(type)){
                                queryProvinces();
                            } else if("city".equals(type)){
                                queryCities();
                            } else if("county".equals(type)){
                                queryCounties();
                            }
                        }


                    });
                }
            }
        });
    }

    private void closeProgressDialog() {
         if(progressDialog != null){
             progressDialog.dismiss();
         }
    }

    private void showProggressDialog() {
        if(progressDialog == null){
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage("正在加载...");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }

    /**
     * 查询选中省内所有市
     */
    private void queryCities() {
        titleText.setText(selectedProvince.getProvinceName());
        backButton.setVisibility(View.VISIBLE);
        cityList =DataSupport.where("provinceid =?",String.valueOf(selectedProvince.getId())) .find(City.class);
        if(cityList.size() >0){
            dataList.clear();
            for (City city :cityList){
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel= LEVEL_CITY;
        }  else {
            int provinceCode = selectedProvince.getProvinceCode();
            String address ="http://guolin.tech/api/china/"+provinceCode;
            queryFromServer(address,"city");
        }

    }

    private void queryCounties() {
        titleText.setText(selectedCity.getCityName());
        backButton.setVisibility(View.VISIBLE);

        countyList =DataSupport.where("cityid =?",String.valueOf(selectedCity.getId())).find(County.class);

        if(countyList.size() >0){
            dataList.clear();
            for (County county : countyList){
                dataList.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_COUNTY ;
        }else {
              int provinceCode = selectedProvince.getProvinceCode();
                int cityCode = selectedCity.getCityCode();
            String address = "http://guolin.tech/api/china/"+provinceCode+"/"+cityCode;
            queryFromServer(address,"county");
        }
    }
}

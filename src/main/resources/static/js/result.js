var items;
var target_page;        //当前在哪页
var item_num;           //可以显示多少条

function GetQueryString(name) {
    var reg = new RegExp("(^|&)" + name + "=([^&]*)(&|$)");
    var r = window.location.search.substr(1).match(reg);
    if (r != null)
        return decodeURI(r[2]);
    return null;
}

function show_new() {
    items.state.new_or_all='new';
    $.ajax({
        type: "POST",
        url: "getItemNum",
        contentType: "application/json;charset=utf-8",
        data:JSON.stringify(items.state),
        success:function(data,statu){
            item_num=data.item_num;
            var origin_page=target_page;
            target_page=1;
            clearPage();
            getPage(1,origin_page);
            $("#pagen a").text(item_num%10==0?item_num/10:Math.ceil(item_num/10));
            regulatePage(1);
        }
    });
}

function show_all(){
    items.state.new_or_all='all';
    $.ajax({
        type: "POST",
        url: "getItemNum",
        contentType: "application/json;charset=utf-8",
        data:JSON.stringify(items.state),
        success:function(data,statu){
            item_num=data.item_num;
            var origin_page=target_page;
            target_page=1;
            clearPage();
            getPage(target_page,origin_page);
            $("#pagen a").text(item_num%10==0?item_num/10:Math.ceil(item_num/10));
            regulatePage(1);
        }
    });
}

function add_items() {
    clearPage();
    $.ajax({
        type: "POST",
        url: "getPage?field=" + GetQueryString("field")+"&name="+GetQueryString("name")+"&page="+target_page+"&origin="+target_page,
        contentType: "application/json;charset=utf-8",
        data: JSON.stringify(items),
        success: function (data) {
            $.post("result.html",function (data) {
                if(data!=null){
                    $(window).attr('location','fieldlib.html?field='+GetQueryString("field"));
                }
            });
        },
        error: function (message) {
            console.log(message);
        }
    });
    
}

function getPage(page,origin){
    if(items["item_num"]==null)
        items["item_num"]=(target_page-1)*10+items["item"].length;
    $.ajax({
        type: "POST",
        url: "getPage?field=" + GetQueryString("field")+"&name="+GetQueryString("name")+"&page="+page+"&origin="+origin,
        contentType: "application/json;charset=utf-8",
        data: JSON.stringify(items),
        success: function (data) {
            var count=0;
            items["item"]=data["item"];
            for(var i=0; i<10 && i<data["item"].length;i++){
                if (data["item"][i]["isnew"]) {          //高亮显示
                    $("#result_table").append($('<tr>' +
                        '                           <td class="order" style="color: #1e87f0">' + ((page-1)*10+i+1) + '</td>' +
                        '                           <td class="entity" style="color: #1e87f0">' + items["item"][i]["entity"] + '</td>' +
                        '                           <td class="point" style="color: #1e87f0">' + items["item"][i]["point"] + '</td>' +
                        '                           <td class="select"><input onclick="selectBox(this)" class="form-check-input select_item" type="checkbox"/></td>' +
                        '                       </tr>'));
                } else {
                    $("#result_table").append($('<tr>' +
                        '                           <td class="order">' + ((page-1)*10+i+1) + '</td>' +
                        '                           <td class="entity">' + items["item"][i]["entity"] + '</td>' +
                        '                           <td class="point">' + items["item"][i]["point"] + '</td>' +
                        '                           <td class="select"><input onclick="selectBox(this)" class="form-check-input select_item" type="checkbox"/></td>' +
                        '                       </tr>'));
                }
                if(data["item"][i]["selected"]==true){
                    $($(".select_item")[i]).prop("checked",true);
                    count++;
                }
            }
            if(count==data["item"].length && $("input[name='select_table']").prop("checked")==false){
                $("input[name='select_page']").prop("checked",true);
            }
            else
                $("input[name='select_page']").prop("checked",false);

        },
        error: function (message) {
            console.log(message);
        }
    });

}



function clearPage(){
    $("#result_table tr").each(function () {
        if($(this).children().first().prop("tagName")=="TD"){
            $(this).remove();
        }
    });
    $("ul[class='uk-tab'] li").each(function () {
        $(this).attr("class", "");
    });
}

function regulatePage(target_page){
    if(item_num==undefined)
        $("#pagen a").text("");
    else
        $("#pagen a").text(item_num %10==0 ? item_num/10 : Math.ceil(item_num/10));
    if(target_page!=1){
        $("ul[class='uk-tab'] li").each(function () {
            if($(this).attr("id")!="pagen") {
                if ($("ul[class='uk-tab'] li").index(this) == 0) {
                    $(this.firstChild).text(target_page - 1);
                    $(this).attr("class", "");
                } else if ($("ul[class='uk-tab'] li").index(this) == 1) {
                    $(this.firstChild).text(target_page);
                    $(this).attr("class", "uk-active");
                } else if ($("ul[class='uk-tab'] li").index(this) == 2) {
                    if (target_page + 1 > parseInt($("#pagen").text())) {
                        $(this.firstChild).text("");
                    } else {
                        $(this.firstChild).text(target_page + 1);
                    }
                } else if ($("ul[class='uk-tab'] li").index(this) == 3) {
                    if (target_page + 2 > parseInt($("#pagen").text())) {
                        $(this.firstChild).text("");
                    } else {
                        $(this.firstChild).text(target_page + 2);
                    }
                } else if ($("ul[class='uk-tab'] li").index(this) == 4) {
                    if (target_page + 3 > parseInt($("#pagen").text())) {
                        $(this.firstChild).text("");
                    } else {
                        $(this.firstChild).text(target_page + 3);
                    }
                }
            }
        });
    }
    else{
        $("ul[class='uk-tab'] li").each(function () {
            if($(this).attr("id")!="pagen") {
                if ($("ul[class='uk-tab'] li").index(this) == 0) {
                    $(this.firstChild).text(1);
                    $(this).attr("class", "uk-active");
                } else if ($("ul[class='uk-tab'] li").index(this) == 1) {
                    if (2 > parseInt($("#pagen").text())) {
                        $(this.firstChild).text("");
                    } else {
                        $(this.firstChild).text(2);
                    }
                    $(this).attr("class", "");
                } else if ($("ul[class='uk-tab'] li").index(this) == 2) {
                    if (3 > parseInt($("#pagen").text())) {
                        $(this.firstChild).text("");
                    } else {
                        $(this.firstChild).text(3);
                    }
                } else if ($("ul[class='uk-tab'] li").index(this) == 3) {
                    if (4 > parseInt($("#pagen").text())) {
                        $(this.firstChild).text("");
                    } else {
                        $(this.firstChild).text(4);
                    }
                } else if ($("ul[class='uk-tab'] li").index(this) == 4) {
                    if (5 > parseInt($("#pagen").text())) {
                        $(this.firstChild).text("");
                    } else {
                        $(this.firstChild).text(5);
                    }
                }
            }
        });
    }
}

function selectBox(obj){
    var index=$("td[class='select']").index($(obj).parent());
    if($(obj).prop("checked")){        //选中
        items.item[index].selected=true;
    }
    else{
        items.item[index].selected=false;
    }
}

$(function () {
    target_page=1;     //初始时第一页


    $.ajax({
        type:"POST",
        url:"result_data?field="+GetQueryString("field")+"&name="+GetQueryString("name"),
        contentType: "application/json;charset=utf-8",
        success:function (data, status) {
            items = data;
            var state={};
            state.filter="全选";  //默认全选
            state.new_or_all="all";     //默认显示所有词
            state.item_num=0;
            items.state=state;
            var i;
            for (i = 0; i < 10 && i < items["item"].length; i++) {
                if (items["item"][i]["isnew"]) {          //高亮显示
                    $("#result_table").append($('<tr>' +
                        '                           <td class="order" style="color: #1e87f0">' + (i + 1) + '</td>' +
                        '                           <td class="entity" style="color: #1e87f0">' + items["item"][i]["entity"] + '</td>' +
                        '                           <td class="point" style="color: #1e87f0">' + items["item"][i]["point"] + '</td>' +
                        '                           <td class="select"><input onclick="selectBox(this)" class="select_item" type="checkbox"/></td>' +
                        '                       </tr>'));
                } else {
                    $("#result_table").append($('<tr>' +
                        '                           <td class="order">' + (i + 1) + '</td>' +
                        '                           <td class="entity">' + items["item"][i]["entity"] + '</td>' +
                        '                           <td class="point">' + items["item"][i]["point"] + '</td>' +
                        '                           <td class="select"><input onclick="selectBox(this)" class="select_item" type="checkbox"/></td>' +
                        '                       </tr>'));
                }
            }
            regulatePage(1);
            /*统计有多少条目*/
            $.ajax({
                type: "POST",
                url: "getItemNum",
                contentType: "application/json;charset=utf-8",
                data:JSON.stringify(items.state),
                success:function(data,statu){
                    item_num=data.item_num;
                    items["item_num"]=item_num;
                    $("#pagen a").text(item_num%10==0?item_num/10:Math.ceil(item_num/10));
                    regulatePage(1);
                }
            });


        }
    });

    //点击切换页面标签
    $("ul[class='uk-tab'] li").each(function () {
        $(this).click(function () {
            if($(this).attr("id")!="pagex" && $(this.firstChild).text()!="" && $(this.firstChild).text()!="NaN") {
                clearPage();
                var origin_page=target_page;
                target_page=parseInt($(this).text());
                getPage(target_page,origin_page);
                regulatePage(target_page);
            }
        });
    });

    $("#goto_page").click(function(){
        if($("#goto").val().length>0) {
            clearPage();
            var origin_page=target_page;
            target_page=$("#goto").val();
            getPage(target_page,origin_page);
            regulatePage(parseInt($("#goto").val()));
        }
    });

    $("#goto").focus(function(){
        this.select();
        if($("#goto").val().length>0)
        $(document).keyup(function(e){
            if(e.keyCode==13){
                clearPage();
                var origin_page=target_page;
                target_page=$("#goto").val();
                getPage(target_page,origin_page);
                regulatePage(parseInt($("#goto").val()));
            }
        });
    });

    $("#confirm").click(function () {
        if($("select").val()==1){
            /*改状态*/
            items.state.filter='全选';
            items.state.filter_num=0;
        }
        else if($("select").val()==2){      //按排序
            items.state.filter='按排序';
            items.state.filter_num=$("#filter").val();
        }
        else if($("select").val()==3){   //按分数
            items.state.filter='按分数';
            items.state.filter_num=$("#filter").val();
        }
        /*统计有多少条目*/
        $.ajax({
            type: "POST",
            url: "getItemNum",
            contentType: "application/json;charset=utf-8",
            data:JSON.stringify(items.state),
            success:function(data,statu){
                item_num=data.item_num;
                clearPage();
                var origin_page=target_page;
                target_page=1;
                getPage(target_page,origin_page);
                $("#pagen a").text(item_num%10==0?item_num/10:Math.ceil(item_num/10));
                regulatePage(1);
            }
        });

    });

    $("input[name='select_page']").change(function () {     //本页全选
        if($(this).prop("checked")==true){            //选中
            $("input[name='select_table']").prop("checked",false);
            var i;
            for(i=0;i<items["item"].length;i++){
                items.item[i].selected=true;
            }
            clearPage();
            getPage(target_page,target_page);
        }
        else{
            var i;
            for(i=0;i<items["item"].length;i++){
                items.item[i].selected=false;
            }
            clearPage();
            getPage(target_page,target_page);
        }

    });

    $("input[name='select_table']").change(function () {        //本表全选
        var flag;
        if($(this).prop("checked")==true){            //选中
            $("input[name='select_page']").prop("checked",false);

            var i;
            for(i=0;i<items["item"].length;i++){
                items["item"][i]["selected"]=true;
            }
            flag=1;
        }
        else{
            var i;
            for(i=0;i<items["item"].length;i++){
                items["item"][i]["selected"]=false;
            }
            flag=0;
        }
        $.post("select_table?&flag="+flag+"&total_length="+item_num,function (data,statu) {
            clearPage();
            getPage(target_page,target_page);
        });

    });

    $("#filter").focus(function () {
        this.select();
    });

});
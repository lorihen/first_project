app.controller('searchController', function ($scope,$location, searchService) {
    //定义搜索对象的结构
    $scope.searchMap = {
        'keywords': '',
        'category': '',
        'brand': '',
        spec: {},
        'price': '',
        'pageNo': 1,
        'pageSize': 40,
        'sort': '',
        'sortField': ''
    };

    //搜索
    $scope.search = function () {
        $scope.searchMap.pageNo = parseInt($scope.searchMap.pageNo);

        searchService.search($scope.searchMap).success(
            function (response) {
                $scope.resultMap = response;//搜索返回的结果

                //构建分页栏
                buildPageLabel();
            }
        );
    };

    //构建分页栏
    buildPageLabel = function () {
        $scope.pageLabel = [];
        var firstPage = 1;//开始页码
        var lastPage = $scope.resultMap.totalPages;//截至页码

        $scope.firstDot = true;//页码前面有点
        $scope.lastDot = true;//页码后面有点

        if ($scope.resultMap.totalPages > 5) {
            //如果总页数大于5
            if ($scope.searchMap.pageNo <= 3) {
                //如果当前页码小于等于3.显示前5页
                lastPage = 5;
                $scope.firstDot = false;//页码前面无点
            } else if ($scope.searchMap.pageNo >= $scope.resultMap.totalPages - 2) {
                //如果当前页大于等于最大页码-2
                firstPage = $scope.resultMap.totalPages - 4;
                $scope.lastDot = false;//页码后面无点
                alert(firstPage);
                alert(lastPage);
            } else {
                firstPage = $scope.searchMap.pageNo - 2;
                lastPage = $scope.searchMap.pageNo + 2;
            }
        } else {
            //如果总页数小于5
            $scope.firstDot = false;//页码前面无点
            $scope.lastDot = false;//页码后面无点
        }

        for (var i = firstPage; i <= lastPage; i++) {
            $scope.pageLabel.push(i);
        }
    };

    //添加搜索项  改变searchMap的值
    $scope.addSearchItem = function (key, value) {
        if (key == 'category' || key == 'brand' || key == 'price') {
            //如果用户点击的是分类或者品牌
            $scope.searchMap[key] = value;
        } else {
            //如果用户点击的是规格
            $scope.searchMap.spec[key] = value;
        }
        $scope.search();//查询

    };

    //撤销搜索项  改变searchMap的值
    $scope.removeSearchItem = function (key) {
        if (key == 'category' || key == 'brand' || key == 'price') {
            //如果用户点击的是分类或者品牌
            $scope.searchMap[key] = "";
        } else {
            //如果用户点击的是规格
            delete $scope.searchMap.spec[key];
        }
        $scope.search();//查询
    };

    //分页查询
    $scope.queryByPage = function (pageNo) {
        //页码验证
        if (pageNo < 1 || pageNo > $scope.resultMap.totalPages) {
            return;
        }

        $scope.searchMap.pageNo = pageNo;
        $scope.search();
    };

    //判断当前页是否为第一页
    $scope.isTopPage = function () {
        if ($scope.searchMap.pageNo == 1) {
            return true;
        } else {
            return false;
        }
    };

    //判断当前页是否为最后一页
    $scope.isEndPage = function () {
        if ($scope.searchMap.pageNo == $scope.resultMap.totalPages) {
            return true;
        } else {
            return false;
        }
    };

    //设置排序规则
    $scope.sortSearch = function (sortField, sort) {
        $scope.searchMap.sort = sort;
        $scope.searchMap.sortField = sortField;
        $scope.search();
    };

    //判断关键字是不是品牌
    $scope.keywordsIsBrand=function(){
        for(var i=0;i<$scope.resultMap.brandList.length;i++){
            if($scope.searchMap.keywords.indexOf($scope.resultMap.brandList[i].text)>=0){
                //如果包含
                return true;
            }
        }
        return false;
    }

    //加载关键字
    $scope.loadkeywords=function () {
        $scope.searchMap.keywords=$location.search()['keywords'];
        $scope.search();
    }
});

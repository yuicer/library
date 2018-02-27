//不改变原数组的冒泡选择
//返回一个新数组
function select(arr) {
  var arr1 = [];
  for (var i = 0, l = arr.length; i < l; i++) {
    for (var j = 0; j < l - i; j++) {
      if (arr[j + 1] < arr[j]) {
        var flag = arr[j];
        arr[j] = arr[j + 1];
        arr[j + 1] = flag;
      }
    }
    arr1[l - i - 1] = arr[l - i - 1];
  }
  return arr1;

}
//不改变原数组的快排
//返回新数组
function quick_sort(arr) {
  if (arr.length <= 1)
    return arr;

  var mid_shu = arr[0],
    left = [],
    right = [];

  for (var i = 1, l = arr.length; i < l; i++) {
    if (arr[i] <= mid_shu)
      left.push(arr[i]);
    else
      right.push(arr[i]);
  }

  return quick_sort(left).concat(mid_shu, quick_sort(right));

}

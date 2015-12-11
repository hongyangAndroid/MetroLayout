package com.zhy.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import com.zhy.metrolayout.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by zhy on 15/12/10.
 */
public class MetroLayout extends ViewGroup
{
    private static class MetroBlock
    {
        public int left;
        public int top;
        public int width;
    }

    private List<MetroBlock> mAvailablePos = new ArrayList<MetroBlock>();

    private float mWidthDivider;
    private float mHeightDivider;

    public MetroLayout(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MetroLayout);
        mWidthDivider = caculateFraction(a.getString(R.styleable.MetroLayout_metro_horizon_divider));
        mHeightDivider = caculateFraction(a.getString(R.styleable.MetroLayout_metro_vertical_divider));
        a.recycle();

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (true)
            randomColor();

        for (int i = 0, n = getChildCount(); i < n; i++)
        {
            View v = getChildAt(i);
            LayoutParams lp = (LayoutParams) v.getLayoutParams();

            lp.width = (int) (Math.ceil(getMeasuredWidth() * lp.widthPercent));
            lp.height = (int) (Math.ceil(getMeasuredHeight() * lp.heightPercent));

            measureChild(v, widthMeasureSpec, heightMeasureSpec);
        }


    }

    private void randomColor()
    {
        Random r = new Random(255);

        for (int i = 0, n = getChildCount(); i < n; i++)
        {
            View v = getChildAt(i);

            v.setBackgroundColor(Color.argb(100, r.nextInt(), r.nextInt(), r.nextInt()));
        }
    }


    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b)
    {

        initAvailablePosition();

        int left = 0;
        int top = 0;
        int horizonDivider = (int) Math.ceil(mWidthDivider * getMeasuredWidth());
        int verticalDivider = (int) Math.ceil(mHeightDivider * getMeasuredHeight());

        for (int i = 0, n = getChildCount(); i < n; i++)
        {
            View v = getChildAt(i);
            if (v.getVisibility() == View.GONE) continue;

            MetroBlock newPos = findAvailablePos(v);
            left = newPos.left;
            top = newPos.top;

            int childWidth = v.getMeasuredWidth();
            int childHeight = v.getMeasuredHeight();

            int right = left + childWidth;
            int bottom = top + childHeight;

            v.layout(left, top, right, bottom);

            if (childWidth + horizonDivider < newPos.width)
            {
                newPos.left += childWidth + horizonDivider;
                newPos.width -= childWidth + horizonDivider;
            } else
            {
                mAvailablePos.remove(newPos);
            }

            MetroBlock p = new MetroBlock();
            p.left = left;
            p.top = bottom + verticalDivider;
            p.width = childWidth;
            mAvailablePos.add(p);

            mergeAvailablePosition();

        }
    }

    private void mergeAvailablePosition()
    {
        if (mAvailablePos.size() <= 1) return;

        List<MetroBlock> copy = new ArrayList<>();

        MetroBlock one = mAvailablePos.get(0);
        MetroBlock two = mAvailablePos.get(1);

        for (int i = 1, n = mAvailablePos.size(); i < n - 1; i++)
        {
            if (one.top == two.top)
            {
                one.width = one.width + two.width;
                copy.add(one);
                two.left = one.left;
                two = mAvailablePos.get(i + 1);
            } else
            {
                one = mAvailablePos.get(i);
                two = mAvailablePos.get(i + 1);
            }
        }

        mAvailablePos.removeAll(copy);

    }

    private void initAvailablePosition()
    {
        mAvailablePos.clear();
        MetroBlock first = new MetroBlock();
        first.left = getPaddingLeft();
        first.top = getPaddingTop();
        first.width = getMeasuredWidth();
        mAvailablePos.add(first);
    }

    private MetroBlock findAvailablePos(View view)
    {
        MetroBlock p = new MetroBlock();
        if (mAvailablePos.size() == 0)
        {
            p.left = getPaddingLeft();
            p.top = getPaddingTop();
            p.width = getMeasuredWidth();
            return p;
        }
        int min = mAvailablePos.get(0).top;
        MetroBlock minHeightPos = mAvailablePos.get(0);
        for (MetroBlock _p : mAvailablePos)
        {
            if (_p.top < min)
            {
                min = _p.top;
                minHeightPos = _p;
            }
        }
        return minHeightPos;
    }


    @Override
    public MetroLayout.LayoutParams generateLayoutParams(AttributeSet attrs)
    {
        return new LayoutParams(getContext(), attrs);
    }

    public static class LayoutParams extends ViewGroup.LayoutParams
    {
        public float widthPercent;
        public float heightPercent;

        public LayoutParams(Context c, AttributeSet attrs)
        {
            super(c, attrs);

            TypedArray array = c.obtainStyledAttributes(attrs, R.styleable.MetroLayout_Layout);
            int n = array.getIndexCount();

            for (int i = 0; i < n; i++)
            {
                int index = array.getIndex(i);
                String val = "";
                switch (index)
                {
                    case R.styleable.MetroLayout_Layout_layout_metro_width:
                        val = array.getString(index);
                        widthPercent = caculateFraction(val);
                        break;
                    case R.styleable.MetroLayout_Layout_layout_metro_height:
                        val = array.getString(index);
                        heightPercent = caculateFraction(val);
                        break;
                }
            }

            validParams();
        }

        private void validParams()
        {
            if (widthPercent <= 0 || heightPercent <= 0)
            {
                throw new IllegalArgumentException("layout_metro_width and layout_metro_height must be set as format: %d/%d , such as 1/3 .");

            }
        }


    }

    private static float caculateFraction(String fractionStr)
    {
        String[] split = fractionStr.split("/");

        if (split.length != 2)
        {
            throw new IllegalArgumentException(fractionStr + " must be format as %d/%d , such as 1/3 .");
        }

        String numerator = split[0];
        String denominator = split[1];

        try
        {
            return Integer.parseInt(numerator) * 1.0f / Integer.parseInt(denominator);
        } catch (NumberFormatException e)
        {
            throw new IllegalArgumentException(fractionStr + " must be format as %d/%d , such as 1/3 .");
        }
    }

}

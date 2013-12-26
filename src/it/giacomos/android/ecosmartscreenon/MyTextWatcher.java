package it.giacomos.android.ecosmartscreenon;
import android.text.Editable;
import android.text.TextWatcher;

public class MyTextWatcher implements TextWatcher {

	private EditTextListener mEditTextListener;
	
	public MyTextWatcher(EditTextListener etl)
	{
		mEditTextListener = etl;
	}
	
	@Override
	public void afterTextChanged(Editable s) 
	{
		int value = 80000;
		try
		{
			value = Integer.parseInt(s.toString());
			mEditTextListener.onEditTextValueChanged(value);
		}
		catch(NumberFormatException e)
		{
			
		}
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		// TODO Auto-generated method stub
		
	}

}
